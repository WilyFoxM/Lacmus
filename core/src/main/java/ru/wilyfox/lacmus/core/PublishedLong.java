package ru.wilyfox.lacmus.core;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/** A value set by one thread and read by others: a release store and an acquire load. */
public final class PublishedLong {
  private static final VarHandle VALUE;

  static {
    try {
      VALUE = MethodHandles.lookup().findVarHandle(PublishedLong.class, "value", long.class);
    } catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @SuppressWarnings("unused") // accessed through VALUE
  private long value;

  public PublishedLong(long initial) {
    value = initial;
  }

  /** Writer thread only. */
  public void set(long newValue) {
    VALUE.setRelease(this, newValue);
  }

  /** Any thread. */
  public long get() {
    return (long) VALUE.getAcquire(this);
  }
}
