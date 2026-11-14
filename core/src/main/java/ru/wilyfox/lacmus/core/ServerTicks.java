package ru.wilyfox.lacmus.core;

/**
 * An estimate of how fast the server ticks, from the time packets it sends anyway.
 *
 * <p>The server sends its game time every 20 of its ticks, and the game time advances once per tick
 * unless ticking is frozen. The arrival of each time packet is stamped on the network thread; the
 * estimate is the change of game time over the arrival times within a window of several seconds.
 *
 * <p>A packet can arrive late but never early: a proxy switching servers, or a stall, holds some
 * back and then delivers them together with the next ones. Measured from a held-up packet, the
 * estimate would overshoot. So the start of the measurement is the oldest packet that is not
 * noticeably later than the most punctual packet of the window, judged against the server's own
 * schedule at its target rate. A server that really ticks faster than its target, which makes every
 * newer packet look earlier, is told apart by its shorter steps between packets. The newest packet
 * is always the end: a late newest packet does mean late ticks.
 *
 * <p>The tick rate the server aims at and whether it is frozen come from its ticking-state packet.
 *
 * <p>Network side: {@link #timeSync}, {@link #tickingState}. Reader side: the rest.
 */
public final class ServerTicks {
  /** The server sends its time every this many ticks. */
  public static final int SYNC_TICKS = 20;

  /** The game's default tick rate, assumed until the server says otherwise. */
  public static final float DEFAULT_TICK_RATE = 20;

  /**
   * Averaging window of the estimate, or one interval more than the shortest span when the server
   * ticks slowly.
   */
  public static final long WINDOW_NANOS = 5_000_000_000L;

  /**
   * The estimate spans at least this many intervals between time packets: over a short span, a
   * packet held back within {@link #HELD_UP_NANOS} still moves it noticeably.
   */
  public static final int MIN_SPAN_INTERVALS = 3;

  /**
   * A packet this much later than the most punctual one is held up, not just jittered. Over the
   * shortest span this lifts the estimate by at most about 1.7 %.
   */
  public static final long HELD_UP_NANOS = 50_000_000L;

  private static final int SAMPLES = 32;

  /** Packets that arrive within this share of their expected step arrived together. */
  private static final double BUNCHED_SHARE = 0.1;

  /** A median step below this share of the expected step means the server ticks faster. */
  private static final double FASTER_SHARE = 0.8;

  /** A time packet later than this share of its interval counts as late. */
  public static final double LATE_SHARE = 0.5;

  private static final long NO_STATE = Long.MIN_VALUE;

  // Network side.
  private final PublishedPair syncs = new PublishedPair();
  private final PublishedLong state = new PublishedLong(NO_STATE);

  // Reader side.
  private final long[] gameTimes = new long[SAMPLES];
  private final long[] arrivals = new long[SAMPLES];
  private final double[] stepScratch = new double[SAMPLES];
  private int start;
  private int count;
  private double estimate = Double.NaN;
  private long lastVersion;
  private long lastSyncNanos;
  private boolean hasSync;
  private long now;

  /** A time packet with {@code gameTime} arrived at {@code nanos}. Network thread. */
  public void timeSync(long gameTime, long nanos) {
    syncs.set(gameTime, nanos);
  }

  /** The server aims at {@code tickRate} ticks a second and is or is not frozen. Network thread. */
  public void tickingState(float tickRate, boolean frozen) {
    state.set(((long) Float.floatToRawIntBits(tickRate) << 1) | (frozen ? 1 : 0));
  }

  /** Reader thread. */
  public void update(long nanos) {
    now = nanos;
    long version = syncs.read();
    if (version != 0 && version != lastVersion) {
      lastVersion = version;
      lastSyncNanos = syncs.second();
      hasSync = true;
      add(syncs.first(), syncs.second());
      estimate = estimate();
    }
  }

  /** Estimated ticks per second, or NaN until the time packets span two intervals. */
  public double ticksPerSecond() {
    return estimate;
  }

  private void add(long gameTime, long arrival) {
    if (count > 0 && gameTime < gameTimes[index(count - 1)]) {
      // Game time never goes back on one server: start over.
      count = 0;
    }
    if (count == SAMPLES) {
      start = (start + 1) % SAMPLES;
      count--;
    }
    gameTimes[index(count)] = gameTime;
    arrivals[index(count)] = arrival;
    count++;
    // Longer than the shortest span by an interval, so jitter never drops the start it needs.
    long window = Math.max(WINDOW_NANOS, (MIN_SPAN_INTERVALS + 1) * syncIntervalNanos());
    while (count > 1 && arrival - arrivals[start] > window) {
      start = (start + 1) % SAMPLES;
      count--;
    }
  }

  private double estimate() {
    int end = index(count - 1);
    double rate = targetTickRate() > 0 ? targetTickRate() : DEFAULT_TICK_RATE;
    long minSpan = MIN_SPAN_INTERVALS * syncIntervalNanos();
    // How late each packet is against a schedule at the target rate, judged against every packet
    // of the window, the newest included: a start that was held up can only be told from the
    // packets that came on time after it.
    double punctual = Double.POSITIVE_INFINITY;
    for (int i = 0; i < count; i++) {
      punctual = Math.min(punctual, lateness(index(i), rate));
    }
    for (int i = 0; i < count - 1; i++) {
      int k = index(i);
      if (arrivals[end] - arrivals[k] >= minSpan && lateness(k, rate) <= punctual + HELD_UP_NANOS) {
        return rateBetween(k, end);
      }
    }
    // No start is as punctual as the newer packets. Either the older packets were held up, and
    // the estimate waits for the punctual ones to age, or the server really ticks faster than its
    // target, so every newer packet is "earlier": then the packets come at shorter steps.
    if (fasterThanTarget(rate)) {
      for (int i = 0; i < count - 1; i++) {
        int k = index(i);
        if (arrivals[end] - arrivals[k] >= minSpan) {
          return rateBetween(k, end);
        }
      }
    }
    return Double.NaN;
  }

  private double rateBetween(int from, int to) {
    return (gameTimes[to] - gameTimes[from]) * 1e9 / (arrivals[to] - arrivals[from]);
  }

  /**
   * Whether the packets come at shorter steps than the target rate gives, judged by the median
   * step. Steps of packets that arrived together after a hold-up are left out: they say nothing
   * about the server's pace.
   */
  private boolean fasterThanTarget(double rate) {
    double expected = 1e9 / rate;
    double[] steps = stepScratch;
    int n = 0;
    for (int i = 1; i < count; i++) {
      int a = index(i - 1);
      int b = index(i);
      long ticks = gameTimes[b] - gameTimes[a];
      if (ticks <= 0) {
        continue;
      }
      double perTick = (arrivals[b] - arrivals[a]) / (double) ticks;
      if (perTick >= expected * BUNCHED_SHARE) {
        steps[n++] = perTick;
      }
    }
    if (n == 0) {
      return false;
    }
    java.util.Arrays.sort(steps, 0, n);
    return steps[n / 2] < expected * FASTER_SHARE;
  }

  private double lateness(int k, double rate) {
    return arrivals[k] - gameTimes[k] / rate * 1e9;
  }

  private int index(int i) {
    return (start + i) % SAMPLES;
  }

  /** The tick rate the server aims at: from its ticking state, or the default. */
  public float targetTickRate() {
    long s = state.get();
    return s == NO_STATE ? DEFAULT_TICK_RATE : Float.intBitsToFloat((int) (s >>> 1));
  }

  /** Whether the server said its ticking is frozen. */
  public boolean frozen() {
    long s = state.get();
    return s != NO_STATE && (s & 1) != 0;
  }

  /** How often the server sends its time at its target tick rate. */
  public long syncIntervalNanos() {
    float rate = targetTickRate();
    if (!(rate > 0)) {
      rate = DEFAULT_TICK_RATE;
    }
    return (long) (SYNC_TICKS / (double) rate * 1e9);
  }

  /**
   * How late the next time packet is, in milliseconds, once it is later than {@link #LATE_SHARE} of
   * its interval; otherwise, and before the first time packet, NaN.
   */
  public double lateMillis() {
    if (!hasSync) {
      return Double.NaN;
    }
    long interval = syncIntervalNanos();
    long late = now - lastSyncNanos - interval;
    return late > interval * LATE_SHARE ? late / 1e6 : Double.NaN;
  }
}
