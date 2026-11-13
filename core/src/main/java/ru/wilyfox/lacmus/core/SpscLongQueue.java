package ru.wilyfox.lacmus.core;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * A bounded queue of {@code long} values for one producer thread and one consumer thread, with no
 * locks and no allocation after construction. When full, new values are dropped and counted.
 */
public final class SpscLongQueue {
  private static final VarHandle HEAD;
  private static final VarHandle TAIL;

  static {
    try {
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      HEAD = lookup.findVarHandle(SpscLongQueue.class, "head", long.class);
      TAIL = lookup.findVarHandle(SpscLongQueue.class, "tail", long.class);
    } catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private final long[] values;
  private final int mask;

  /** Next slot to read; written by the consumer only. */
  @SuppressWarnings("unused") // accessed through HEAD
  private long head;

  /** Next slot to write; written by the producer only. */
  @SuppressWarnings("unused") // accessed through TAIL
  private long tail;

  private final SingleWriterCounter dropped = new SingleWriterCounter();

  /**
   * @param capacity a power of two
   */
  public SpscLongQueue(int capacity) {
    if (capacity <= 0 || Integer.bitCount(capacity) != 1) {
      throw new IllegalArgumentException("capacity must be a power of two: " + capacity);
    }
    values = new long[capacity];
    mask = capacity - 1;
  }

  /** Producer thread only. Returns {@code false} and counts a drop when the queue is full. */
  public boolean offer(long value) {
    long t = (long) TAIL.get(this);
    if (t - (long) HEAD.getAcquire(this) == values.length) {
      dropped.add(1);
      return false;
    }
    values[(int) (t & mask)] = value;
    TAIL.setRelease(this, t + 1);
    return true;
  }

  /** Consumer thread only. */
  public boolean isEmpty() {
    return (long) HEAD.get(this) == (long) TAIL.getAcquire(this);
  }

  /** Consumer thread only; call after {@link #isEmpty()} returned {@code false}. */
  public long poll() {
    long h = (long) HEAD.get(this);
    if (h == (long) TAIL.getAcquire(this)) {
      throw new IllegalStateException("queue is empty");
    }
    long value = values[(int) (h & mask)];
    HEAD.setRelease(this, h + 1);
    return value;
  }

  /** Values dropped because the queue was full. Any thread. */
  public long dropped() {
    return dropped.get();
  }
}
