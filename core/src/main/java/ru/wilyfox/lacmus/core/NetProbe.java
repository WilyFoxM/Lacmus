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

  private static final int PENDING_PINGS = 16;
  private static final int RTT_QUEUE = 64;
  private static final int RATE_SAMPLES = 64;

  // Network side.
  private final SingleWriterCounter inboundBytes = new SingleWriterCounter();
  private final SingleWriterCounter outboundBytes = new SingleWriterCounter();
  private final SingleWriterCounter inboundPackets = new SingleWriterCounter();
  private final SingleWriterCounter outboundPackets = new SingleWriterCounter();
  private final PendingPings pending = new PendingPings(PENDING_PINGS);
  private final SpscLongQueue roundTrips = new SpscLongQueue(RTT_QUEUE);

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

  public void inboundPacket() {
    inboundPackets.add(1);
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

  // ---- Reader side --------------------------------------------------------------------------

  /** Takes in everything the network side recorded up to now. */
  public void update(long nanos) {
    now = nanos;
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

  /** Round trips dropped because the client thread fell behind. */
  public long droppedRoundTrips() {
    return roundTrips.dropped();
  }

  private boolean rttFresh() {
    return hasRtt && now - lastRttNanos <= RTT_STALE_NANOS;
  }
}
