package ru.wilyfox.lacmus.core;

/**
 * The client's own side: how long its frames and ticks take, and how long packets wait for it. All
 * over the last second (20 client ticks). Client thread, except {@link QueueDelay#queued}.
 */
public final class ClientStats {
  /** Window of the frame and tick statistics, in client ticks. */
  public static final int WINDOW_TICKS = 20;

  /** A client tick that takes this long on average holds the game below 20 ticks a second. */
  public static final long TICK_ALARM_NANOS = 50_000_000L;

  private final QueueDelay queue = new QueueDelay();
  private final WindowStats frames = new WindowStats(WINDOW_TICKS);
  private final WindowStats ticks = new WindowStats(WINDOW_TICKS);

  public QueueDelay queue() {
    return queue;
  }

  /** Time between frames, as the game measures it. */
  public WindowStats frames() {
    return frames;
  }

  /** Time the client's tick takes. */
  public WindowStats ticks() {
    return ticks;
  }

  /** Closes one client tick. */
  public void update() {
    queue.update();
    frames.closeTick();
    ticks.closeTick();
  }

  /** Forgets everything, for a new connection. */
  public void reset() {
    queue.reset();
    frames.clear();
    ticks.clear();
  }
}
