package ru.wilyfox.lacmus.core;

/** The last {@code capacity} samples, oldest overwritten first. Single-threaded. */
public final class SampleWindow {
  private final double[] samples;
  private int start;
  private int size;

  public SampleWindow(int capacity) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("capacity must be positive: " + capacity);
    }
    samples = new double[capacity];
  }

  public void add(double sample) {
    if (size < samples.length) {
      samples[(start + size) % samples.length] = sample;
      size++;
    } else {
      samples[start] = sample;
      start = (start + 1) % samples.length;
    }
  }

  public int size() {
    return size;
  }

  public void clear() {
    start = 0;
    size = 0;
  }

  /** The {@code i}-th sample, oldest first. */
  public double get(int i) {
    if (i < 0 || i >= size) {
      throw new IndexOutOfBoundsException(i);
    }
    return samples[(start + i) % samples.length];
  }

  /** The newest sample, or NaN when empty. */
  public double last() {
    return size == 0 ? Double.NaN : get(size - 1);
  }

  /** Arithmetic mean, or NaN when empty. */
  public double mean() {
    if (size == 0) {
      return Double.NaN;
    }
    double sum = 0;
    for (int i = 0; i < size; i++) {
      sum += get(i);
    }
    return sum / size;
  }

  /**
   * Mean absolute difference between consecutive samples, or NaN with fewer than two samples. For
   * round-trip times this is the jitter: how much one trip differs from the previous one.
   */
  public double meanAbsDelta() {
    if (size < 2) {
      return Double.NaN;
    }
    double sum = 0;
    double previous = get(0);
    for (int i = 1; i < size; i++) {
      double current = get(i);
      sum += Math.abs(current - previous);
      previous = current;
    }
    return sum / (size - 1);
  }
}
