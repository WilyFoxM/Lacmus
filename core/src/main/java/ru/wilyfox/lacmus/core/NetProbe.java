package ru.wilyfox.lacmus.core;

/**
 * Network measurements of one connection.
 *
 * <p>Two sides with one thread each. The <b>network side</b> ({@code inbound*}, {@code outbound*},
 * {@link #pingSent}, {@link #pongReceived}) is called on the connection's network thread, as close
 * to the wire as possible, and only stamps and counts. The <b>reader side</b> ({@link #update} and
 * the getters) is called on the client thread, which turns the counts into rates and the round
 * trips into statistics. A frozen client therefore delays the numbers but does not distort them.
 */
public final class NetProbe {
  /** Averaging window for traffic rates. */
  public static final long RATE_WINDOW_NANOS = 1_000_000_000L;

  /** A round trip older than this is no longer shown. */
  public static final long RTT_STALE_NANOS = 5_000_000_000L;

  /** Round trips kept for the average and the jitter. */
  public static final int RTT_SAMPLES = 16;

  /**
   * Silence shorter than this is normal: a healthy server sends its time once a second, and pongs
   * to the probe's requests arrive twice a second.
   */
  public static final long STALL_NANOS = 1_500_000_000L;

  /** From this much silence on, the time left before the client gives up is shown too. */
  public static final long TIMEOUT_WARNING_NANOS = 5_000_000_000L;

  /** The game client disconnects after this long without incoming data. */
  public static final long CLIENT_TIMEOUT_NANOS = 30_000_000_000L;

  private static final long NONE = Long.MIN_VALUE;

  private static final int PENDING_PINGS = 16;
  private static final int RTT_QUEUE = 64;
  private static final int RATE_SAMPLES = 64;

  // Network side.
  private final SingleWriterCounter inboundBytes = new SingleWriterCounter();
  private final SingleWriterCounter outboundBytes = new SingleWriterCounter();
  private final SingleWriterCounter inboundPackets = new SingleWriterCounter();
  private final SingleWriterCounter outboundPackets = new SingleWriterCounter();
  private final PublishedLong lastInboundNanos = new PublishedLong(NONE);
  private final PendingPings pending = new PendingPings(PENDING_PINGS);
  private final SpscLongQueue roundTrips = new SpscLongQueue(RTT_QUEUE);
  private final ServerTicks ticks = new ServerTicks();

  // Reader side.
  private final RateMeter inboundByteRate = new RateMeter(RATE_WINDOW_NANOS, RATE_SAMPLES);
  private final RateMeter outboundByteRate = new RateMeter(RATE_WINDOW_NANOS, RATE_SAMPLES);
  private final RateMeter inboundPacketRate = new RateMeter(RATE_WINDOW_NANOS, RATE_SAMPLES);
  private final RateMeter outboundPacketRate = new RateMeter(RATE_WINDOW_NANOS, RATE_SAMPLES);
  private final SampleWindow rttMillis = new SampleWindow(RTT_SAMPLES);
  private boolean hasRtt;
  private long lastRttNanos;
  private long now;

  // ---- Network side -------------------------------------------------------------------------

  public void inboundBytes(long bytes) {
    inboundBytes.add(bytes);
  }

  public void outboundBytes(long bytes) {
    outboundBytes.add(bytes);
  }

  /** A packet from the server was read at {@code nanos}. */
  public void inboundPacket(long nanos) {
    inboundPackets.add(1);
    lastInboundNanos.set(nanos);
  }

  public void outboundPacket() {
    outboundPackets.add(1);
  }

  /** A ping request carrying {@code token} is being written at {@code nanos}. */
  public void pingSent(long token, long nanos) {
    pending.sent(token, nanos);
  }

  /** A pong carrying {@code token} was read at {@code nanos}. Unknown tokens are ignored. */
  public void pongReceived(long token, long nanos) {
    long roundTrip = pending.received(token, nanos);
    if (roundTrip >= 0) {
      roundTrips.offer(roundTrip);
    }
  }

  /** A time packet with {@code gameTime} was read at {@code nanos}. */
  public void timeSync(long gameTime, long nanos) {
    ticks.timeSync(gameTime, nanos);
  }

  /** The server's ticking state: its target tick rate and whether it is frozen. */
  public void tickingState(float tickRate, boolean frozen) {
    ticks.tickingState(tickRate, frozen);
  }

  // ---- Reader side --------------------------------------------------------------------------

  /** Takes in everything the network side recorded up to now. */
  public void update(long nanos) {
    now = nanos;
    ticks.update(nanos);
    inboundByteRate.sample(nanos, inboundBytes.get());
    outboundByteRate.sample(nanos, outboundBytes.get());
    inboundPacketRate.sample(nanos, inboundPackets.get());
    outboundPacketRate.sample(nanos, outboundPackets.get());
    while (!roundTrips.isEmpty()) {
      long roundTrip = roundTrips.poll();
      if (hasRtt && nanos - lastRttNanos > RTT_STALE_NANOS) {
        // A long gap: the old trips say nothing about the new ones.
        rttMillis.clear();
      }
      rttMillis.add(roundTrip / 1e6);
      hasRtt = true;
      lastRttNanos = nanos;
    }
  }

  public double inboundBytesPerSecond() {
    return inboundByteRate.perSecond();
  }

  public double outboundBytesPerSecond() {
    return outboundByteRate.perSecond();
  }

  public double inboundPacketsPerSecond() {
    return inboundPacketRate.perSecond();
  }

  public double outboundPacketsPerSecond() {
    return outboundPacketRate.perSecond();
  }

  /** The newest round trip in milliseconds, or NaN when there is none or it is stale. */
  public double rttMillis() {
    return rttFresh() ? rttMillis.last() : Double.NaN;
  }

  /** Jitter of the recent round trips in milliseconds, or NaN when unknown or stale. */
  public double jitterMillis() {
    return rttFresh() ? rttMillis.meanAbsDelta() : Double.NaN;
  }

  /**
   * How long nothing has arrived from the server, in milliseconds, once that is longer than {@link
   * #stallThresholdNanos()}; otherwise, and before the first packet, NaN.
   */
  public double stallMillis() {
    long silence = silenceNanos();
    return silence >= stallThresholdNanos() ? silence / 1e6 : Double.NaN;
  }

  /**
   * {@link #STALL_NANOS}, or one and a half intervals between the server's time packets when the
   * server ticks so slowly that they come less often than that.
   */
  public long stallThresholdNanos() {
    return Math.max(STALL_NANOS, ticks.syncIntervalNanos() * 3 / 2);
  }

  /** The server's tick rate, from the time packets it sends anyway. */
  public ServerTicks ticks() {
    return ticks;
  }

  /**
   * Milliseconds left before the game client times out, once the silence is longer than {@link
   * #TIMEOUT_WARNING_NANOS}; otherwise NaN. Only meaningful for a connection over a network.
   */
  public double timeoutInMillis() {
    long silence = silenceNanos();
    if (silence < TIMEOUT_WARNING_NANOS) {
      return Double.NaN;
    }
    return Math.max(0, CLIENT_TIMEOUT_NANOS - silence) / 1e6;
  }

  /** Round trips dropped because the client thread fell behind. */
  public long droppedRoundTrips() {
    return roundTrips.dropped();
  }

  private long silenceNanos() {
    long last = lastInboundNanos.get();
    // A packet read after this update's clock reading counts as no silence.
    return last == NONE ? -1 : Math.max(0, now - last);
  }

  private boolean rttFresh() {
    return hasRtt && now - lastRttNanos <= RTT_STALE_NANOS;
  }
}
