package ru.wilyfox.lacmus.core;

/**
 * The server plugin's record of its own ticks: when each tick ended and how long it took, in a
 * fixed ring. Written and read on the server's main thread; no locks, no allocation per tick.
 */
public final class ServerTickLog {
  /** Window for the tick rate. */
  public static final long RATE_WINDOW_NANOS = 5_000_000_000L;

  /** Ticks averaged for the tick duration. */
  public static final int DURATION_TICKS = 20;

  // 5 s at up to ~400 ticks per second; /tick rate goes higher, then the window just gets shorter.
  private static final int CAPACITY = 2048;

  private final long[] ends = new long[CAPACITY];
  private final long[] durations = new long[CAPACITY];
  private long count;
  private long first = Long.MIN_VALUE;

  /** Records a tick that ended at {@code endNanos} and took {@code durationNanos}. */
  public void tick(long endNanos, long durationNanos) {
    int slot = (int) (count % CAPACITY);
    ends[slot] = endNanos;
    durations[slot] = durationNanos;
    if (count == 0) {
      first = endNanos;
    }
    count++;
  }

  /** Forgets every tick, as after the plugin is reloaded. */
  public void clear() {
    count = 0;
    first = Long.MIN_VALUE;
  }

  /** A report of the ticks up to {@code nowNanos}. */
  public ServerReport report(long nowNanos, float targetTickRate, boolean frozen) {
    return new ServerReport(
        targetTickRate, frozen, ticksPerSecond(nowNanos), msptMean(), msptMax());
  }

  /**
   * Ticks per second over the last {@link #RATE_WINDOW_NANOS}: the intervals between the ticks that
   * ended inside it, over the time they span. NaN until the log covers the whole window.
   */
  public double ticksPerSecond(long nowNanos) {
    long from = nowNanos - RATE_WINDOW_NANOS;
    if (count < 2 || first > from) {
      return Double.NaN;
    }
    long held = Math.min(count, CAPACITY);
    long newest = ends[(int) ((count - 1) % CAPACITY)];
    long oldest = newest;
    long inside = 0;
    for (long i = count - 1; i >= count - held; i--) {
      long end = ends[(int) (i % CAPACITY)];
      if (end < from) {
        break;
      }
      inside++;
      oldest = end;
    }
    if (inside < 2 || newest <= oldest) {
      // At most one tick in five seconds: far too slow to call a rate.
      return inside == 0 ? 0 : Double.NaN;
    }
    return (inside - 1) / ((newest - oldest) / 1e9);
  }

  /** Mean duration of the last {@link #DURATION_TICKS} ticks in milliseconds; NaN before any. */
  public double msptMean() {
    long n = Math.min(count, DURATION_TICKS);
    if (n == 0) {
      return Double.NaN;
    }
    long sum = 0;
    for (long i = count - n; i < count; i++) {
      sum += durations[(int) (i % CAPACITY)];
    }
    return sum / 1e6 / n;
  }

  /** Longest of the last {@link #DURATION_TICKS} ticks in milliseconds; NaN before any. */
  public double msptMax() {
    long n = Math.min(count, DURATION_TICKS);
    if (n == 0) {
      return Double.NaN;
    }
    long max = 0;
    for (long i = count - n; i < count; i++) {
      max = Math.max(max, durations[(int) (i % CAPACITY)]);
    }
    return max / 1e6;
  }
}
