package ru.wilyfox.lacmus.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class QueueDelayTest {
  private static final long MS = 1_000_000L;

  @Test
  void unknownWithoutHandledSamples() {
    QueueDelay queue = new QueueDelay();
    queue.update();
    assertTrue(Double.isNaN(queue.worstMillis()));
  }

  @Test
  void measuresTheSampledItemOnly() {
    QueueDelay queue = new QueueDelay();
    Object first = new Object();
    Object second = new Object();
    queue.queued(first, 0);
    // A sample is in flight: the second item is not stamped.
    queue.queued(second, 2 * MS);
    queue.handled(second, 20 * MS);
    queue.update();
    assertTrue(Double.isNaN(queue.worstMillis()));
    queue.handled(first, 7 * MS);
    queue.update();
    assertEquals(7, queue.worstMillis(), 1e-9);
  }

  @Test
  void nextItemIsSampledAfterTheLastWasHandled() {
    QueueDelay queue = new QueueDelay();
    Object a = new Object();
    Object b = new Object();
    queue.queued(a, 0);
    queue.handled(a, 3 * MS);
    queue.queued(b, 10 * MS);
    queue.handled(b, 14 * MS);
    queue.update();
    assertEquals(4, queue.worstMillis(), 1e-9);
  }

  @Test
  void showsTheWorstWaitOfTheLastSecond() {
    QueueDelay queue = new QueueDelay();
    Object frozen = new Object();
    queue.queued(frozen, 0);
    queue.handled(frozen, 1_500 * MS);
    queue.update();
    for (int tick = 1; tick < QueueDelay.WINDOW_TICKS; tick++) {
      Object item = new Object();
      queue.queued(item, tick * MS);
      queue.handled(item, tick * MS + 2 * MS);
      queue.update();
    }
    assertEquals(1_500, queue.worstMillis(), 1e-9);
    Object item = new Object();
    queue.queued(item, 0);
    queue.handled(item, 2 * MS);
    queue.update();
    assertEquals(2, queue.worstMillis(), 1e-9, "the freeze left the window");
  }

  @Test
  void resetDropsAnItemThatWillNeverBeHandled() {
    QueueDelay queue = new QueueDelay();
    queue.queued(new Object(), 0);
    queue.reset();
    Object next = new Object();
    queue.queued(next, 10 * MS);
    queue.handled(next, 13 * MS);
    queue.update();
    assertEquals(3, queue.worstMillis(), 1e-9);
  }
}
