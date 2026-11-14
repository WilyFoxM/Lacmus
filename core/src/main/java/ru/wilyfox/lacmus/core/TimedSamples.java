package ru.wilyfox.lacmus.core;

/** Samples with the time they were taken; the mean of those not older than a window. */
public final class TimedSamples {
  private final long[] times;
  private final double[] values;
  private int start;
  private int size;

  public TimedSamples(int capacity) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("capacity must be positive: " + capacity);
    }
    times = new long[capacity];
    values = new double[capacity];
  }

  public void add(long nanos, double value) {
    int index;
    if (size < times.length) {
      index = (start + size) % times.length;
      size++;
    } else {
      index = start;
      start = (start + 1) % times.length;
    }
    times[index] = nanos;
    values[index] = value;
  }

  /** Mean of the samples taken at or after {@code sinceNanos}, or NaN when there are none. */
  public double meanSince(long sinceNanos) {
    double sum = 0;
    int count = 0;
    for (int i = 0; i < size; i++) {
      int index = (start + i) % times.length;
      if (times[index] >= sinceNanos) {
        sum += values[index];
        count++;
      }
    }
    return count == 0 ? Double.NaN : sum / count;
  }

  public void clear() {
    start = 0;
    size = 0;
  }
}
