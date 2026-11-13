package ru.wilyfox.lacmus.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class QueueAndPingsTest {
  @Test
  void queueKeepsOrderAndDropsWhenFull() {
    SpscLongQueue queue = new SpscLongQueue(2);
    assertTrue(queue.isEmpty());
    assertTrue(queue.offer(1));
    assertTrue(queue.offer(2));
    assertFalse(queue.offer(3));
    assertEquals(1, queue.dropped());
    assertEquals(1, queue.poll());
    assertTrue(queue.offer(4));
    assertEquals(2, queue.poll());
    assertEquals(4, queue.poll());
    assertTrue(queue.isEmpty());
    assertThrows(IllegalStateException.class, queue::poll);
  }

  @Test
  void queueRejectsCapacityThatIsNotAPowerOfTwo() {
    assertThrows(IllegalArgumentException.class, () -> new SpscLongQueue(3));
  }

  @Test
  void queueHandsValuesAcrossThreadsInOrder() throws InterruptedException {
    SpscLongQueue queue = new SpscLongQueue(64);
    int count = 200_000;
    Thread producer =
        new Thread(
            () -> {
              for (long i = 0; i < count; ) {
                if (queue.offer(i)) {
                  i++;
                } else {
                  Thread.onSpinWait();
                }
              }
            });
    producer.start();
    long expected = 0;
    while (expected < count) {
      if (queue.isEmpty()) {
        Thread.onSpinWait();
        continue;
      }
      assertEquals(expected, queue.poll());
      expected++;
    }
    producer.join();
  }

  @Test
  void pingMatchesByTokenOnce() {
    PendingPings pings = new PendingPings(4);
    pings.sent(100, 1_000);
    pings.sent(101, 2_000);
    assertEquals(45_000, pings.received(101, 47_000));
    assertEquals(-1, pings.received(101, 48_000));
    assertEquals(50_000, pings.received(100, 51_000));
    assertEquals(-1, pings.received(999, 51_000));
  }

  @Test
  void pingForgetsOldestWhenSlotsRunOut() {
    PendingPings pings = new PendingPings(2);
    pings.sent(1, 0);
    pings.sent(2, 0);
    pings.sent(3, 0);
    assertEquals(-1, pings.received(1, 10));
    assertEquals(10, pings.received(3, 10));
  }

  @Test
  void counterSumsAdds() {
    SingleWriterCounter counter = new SingleWriterCounter();
    counter.add(5);
    counter.add(7);
    assertEquals(12, counter.get());
  }
}
