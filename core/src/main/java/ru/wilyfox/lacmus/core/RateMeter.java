package ru.wilyfox.lacmus.core;

/**
 * Rate of a running total over a sliding time window, from periodic samples of the total.
 *
 * <p>The rate is the change of the total divided by the time between the newest sample and the
 * newest sample that is at least one window old (or the oldest kept sample, while the history is
 * shorter than the window). Single-threaded.
 */
public final class RateMeter {
  private final long windowNanos;
  private final long[] times;
  private final long[] totals;
  private int start;
  private int size;

  /**
   * @param windowNanos length of the averaging window
   * @param capacity samples kept; must cover the window at the sampling rate
   */
  public RateMeter(long windowNanos, int capacity) {
    if (windowNanos <= 0 || capacity < 2) {
      throw new IllegalArgumentException("window must be positive and capacity at least 2");
    }
    this.windowNanos = windowNanos;
    times = new long[capacity];
    totals = new long[capacity];
  }

  public void sample(long nanos, long total) {
    int index;
    if (size < times.length) {
      index = (start + size) % times.length;
      size++;
    } else {
      index = start;
      start = (start + 1) % times.length;
    }
    times[index] = nanos;
    totals[index] = total;
  }

  public void clear() {
    start = 0;
    size = 0;
  }

  /** Units per second over the window, or NaN before two samples with time between them. */
  public double perSecond() {
    if (size < 2) {
      return Double.NaN;
    }
    int newest = (start + size - 1) % times.length;
    int from = start;
    for (int i = size - 2; i >= 0; i--) {
      int index = (start + i) % times.length;
      if (times[newest] - times[index] >= windowNanos) {
        from = index;
        break;
      }
    }
    long elapsed = times[newest] - times[from];
    if (elapsed <= 0) {
      return Double.NaN;
    }
    return (totals[newest] - totals[from]) * 1e9 / elapsed;
  }
}
