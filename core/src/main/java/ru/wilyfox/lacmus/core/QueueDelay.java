package ru.wilyfox.lacmus.core;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * How long packets wait in the client's queue before the game handles them.
 *
 * <p>Most packets are not handled where they arrive: the network thread puts them in a queue that
 * the client thread works through once per frame. A frozen or overloaded client makes them wait;
 * the network itself does not. One queued item at a time is sampled: the network thread stamps it
 * when it is queued, the client thread measures the wait when it handles that same item. No locks
 * and no allocation.
 *
 * <p>Network side: {@link #queued}. Client side: {@link #handled}, {@link #update} and the getter.
 */
public final class QueueDelay {
  /** The worst wait over this many client ticks (one second at 20 ticks a second) is shown. */
  public static final int WINDOW_TICKS = 20;

  private static final VarHandle SAMPLE;

  static {
    try {
      SAMPLE = MethodHandles.lookup().findVarHandle(QueueDelay.class, "sample", Object.class);
    } catch (ReflectiveOperationException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  /** The sampled item, set by the network thread while empty, cleared by the client thread. */
  @SuppressWarnings("unused") // accessed through SAMPLE
  private Object sample;

  /** Written by the network thread before {@link #sample} is published. */
  private long sampleNanos;

  // Client side.
  private final SampleWindow worstPerTick = new SampleWindow(WINDOW_TICKS);
  private long worstSinceUpdate = -1;

  /** {@code item} was queued at {@code nanos}. Network thread. */
  public void queued(Object item, long nanos) {
    if (SAMPLE.getAcquire(this) == null) {
      sampleNanos = nanos;
      SAMPLE.setRelease(this, item);
    }
  }

  /** {@code item} is being handled at {@code nanos}. Client thread. */
  public void handled(Object item, long nanos) {
    if (item == SAMPLE.getAcquire(this)) {
      worstSinceUpdate = Math.max(worstSinceUpdate, nanos - sampleNanos);
      SAMPLE.setRelease(this, null);
    }
  }

  /** Closes one client tick. Client thread. */
  public void update() {
    worstPerTick.add(worstSinceUpdate);
    worstSinceUpdate = -1;
  }

  /** The longest wait over the last {@link #WINDOW_TICKS} ticks in milliseconds, or NaN. */
  public double worstMillis() {
    long worst = -1;
    for (int i = 0; i < worstPerTick.size(); i++) {
      worst = Math.max(worst, (long) worstPerTick.get(i));
    }
    return worst < 0 ? Double.NaN : worst / 1e6;
  }

  /** Forgets everything, for a new connection. Client thread. */
  public void reset() {
    SAMPLE.setRelease(this, null);
    worstPerTick.clear();
    worstSinceUpdate = -1;
  }
}
