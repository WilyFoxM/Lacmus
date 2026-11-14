package ru.wilyfox.lacmus.core;

/**
 * Durations over the last few client ticks: their mean and their maximum. Values are gathered per
 * tick, so many short ones (frames) and few long ones (a hitch) cost the same: one slot per tick.
 * Single-threaded: the client thread adds and closes ticks.
 */
public final class WindowStats {
  private final long[] sums;
  private final int[] counts;
  private final long[] maxes;
  private int next;
  private int filled;

  private long sum;
  private int count;
  private long max;

  /**
   * @param ticks client ticks in the window
   */
  public WindowStats(int ticks) {
    if (ticks <= 0) {
      throw new IllegalArgumentException("ticks must be positive: " + ticks);
    }
    sums = new long[ticks];
    counts = new int[ticks];
    maxes = new long[ticks];
  }

  /** One duration, in nanoseconds. */
  public void add(long nanos) {
    sum += nanos;
    count++;
    max = Math.max(max, nanos);
  }

  /** Closes the current client tick. */
  public void closeTick() {
    sums[next] = sum;
    counts[next] = count;
    maxes[next] = max;
    next = (next + 1) % sums.length;
    filled = Math.min(filled + 1, sums.length);
    sum = 0;
    count = 0;
    max = 0;
  }

  /** Mean duration over the window in milliseconds, or NaN when nothing was added. */
  public double meanMillis() {
    long total = 0;
    long n = 0;
    for (int i = 0; i < filled; i++) {
      total += sums[i];
      n += counts[i];
    }
    return n == 0 ? Double.NaN : total / (double) n / 1e6;
  }

  /** Longest duration over the window in milliseconds, or NaN when nothing was added. */
  public double maxMillis() {
    long worst = 0;
    long n = 0;
    for (int i = 0; i < filled; i++) {
      worst = Math.max(worst, maxes[i]);
      n += counts[i];
    }
    return n == 0 ? Double.NaN : worst / 1e6;
  }

  public void clear() {
    next = 0;
    filled = 0;
    sum = 0;
    count = 0;
    max = 0;
  }
}
