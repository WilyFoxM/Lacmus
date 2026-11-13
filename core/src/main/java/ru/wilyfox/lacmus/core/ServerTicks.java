package ru.wilyfox.lacmus.core;

/**
 * An estimate of how fast the server ticks, from the time packets it sends anyway.
 *
 * <p>The server sends its game time every 20 of its ticks, and the game time advances once per tick
 * unless ticking is frozen. The arrival of each time packet is stamped on the network thread; the
 * estimate is the change of game time over the arrival times of the last few seconds. Network
 * jitter moves single arrivals, so the window is several seconds long. The tick rate the server
 * aims at and whether it is frozen come from its ticking-state packet.
 *
 * <p>Network side: {@link #timeSync}, {@link #tickingState}. Reader side: the rest.
 */
public final class ServerTicks {
  /** The server sends its time every this many ticks. */
  public static final int SYNC_TICKS = 20;

  /** The game's default tick rate, assumed until the server says otherwise. */
  public static final float DEFAULT_TICK_RATE = 20;

  /** Averaging window of the estimate. */
  public static final long WINDOW_NANOS = 5_000_000_000L;

  /** A time packet later than this share of its interval counts as late. */
  public static final double LATE_SHARE = 0.5;

  private static final long NO_STATE = Long.MIN_VALUE;

  // Network side.
  private final PublishedPair syncs = new PublishedPair();
  private final PublishedLong state = new PublishedLong(NO_STATE);

  // Reader side.
  private final RateMeter gameTimeRate = new RateMeter(WINDOW_NANOS, 16);
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
      gameTimeRate.sample(syncs.second(), syncs.first());
    }
  }

  /** Estimated ticks per second over the window, or NaN before two time packets. */
  public double ticksPerSecond() {
    return gameTimeRate.perSecond();
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
