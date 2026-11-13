package ru.wilyfox.lacmus.core;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * Two values set together by one thread and read together by one other thread, without locks or
 * allocation: a sequence lock. The reader retries while a write is in progress, so it never sees
 * the first value of one write with the second value of another.
 */
public final class PublishedPair {
  private static final VarHandle SEQUENCE;
  private static final VarHandle FIRST;
  private static final VarHandle SECOND;

  static {
    try {
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      SEQUENCE = lookup.findVarHandle(PublishedPair.class, "sequence", long.class);
      FIRST = lookup.findVarHandle(PublishedPair.class, "first", long.class);
      SECOND = lookup.findVarHandle(PublishedPair.class, "second", long.class);
    } catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  /** Even when stable, odd while a write is in progress; 0 before the first write. */
  @SuppressWarnings("unused") // accessed through SEQUENCE
  private long sequence;

  @SuppressWarnings("unused") // accessed through FIRST
  private long first;

  @SuppressWarnings("unused") // accessed through SECOND
  private long second;

  // Reader's copy of the last consistent read.
  private long readFirst;
  private long readSecond;

  /** Writer thread only. */
  public void set(long a, long b) {
    long s = (long) SEQUENCE.get(this);
    SEQUENCE.setOpaque(this, s + 1);
    VarHandle.storeStoreFence();
    FIRST.setOpaque(this, a);
    SECOND.setOpaque(this, b);
    SEQUENCE.setRelease(this, s + 2);
  }

  /**
   * Reader thread only. Takes a consistent copy of both values, available through {@link #first()}
   * and {@link #second()}, and returns its version: 0 before the first write, and a new number
   * after each write.
   */
  public long read() {
    while (true) {
      long before = (long) SEQUENCE.getAcquire(this);
      if ((before & 1) != 0) {
        Thread.onSpinWait();
        continue;
      }
      long a = (long) FIRST.getOpaque(this);
      long b = (long) SECOND.getOpaque(this);
      VarHandle.loadLoadFence();
      if ((long) SEQUENCE.getOpaque(this) == before) {
        readFirst = a;
        readSecond = b;
        return before;
      }
    }
  }

  /** The first value of the last {@link #read()}. */
  public long first() {
    return readFirst;
  }

  /** The second value of the last {@link #read()}. */
  public long second() {
    return readSecond;
  }
}
