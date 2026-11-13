package ru.wilyfox.lacmus.core;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * A running total with exactly one writing thread and any number of readers.
 *
 * <p>The writer adds without a compare-and-set: it alone changes the value, so a plain read
 * followed by a release store is enough. Readers see a value at least as new as the last completed
 * {@link #add}.
 */
public final class SingleWriterCounter {
  private static final VarHandle VALUE;

  static {
    try {
      VALUE = MethodHandles.lookup().findVarHandle(SingleWriterCounter.class, "value", long.class);
    } catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @SuppressWarnings("unused") // accessed through VALUE
  private long value;

  /** Writer thread only. */
  public void add(long delta) {
    VALUE.setRelease(this, (long) VALUE.get(this) + delta);
  }

  /** Any thread. */
  public long get() {
    return (long) VALUE.getAcquire(this);
  }
}
