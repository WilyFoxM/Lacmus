package ru.wilyfox.lacmus.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RateMeterTest {
  private static final long MS = 1_000_000L;

  @Test
  void unknownBeforeTwoSamples() {
    RateMeter meter = new RateMeter(1000 * MS, 8);
    assertTrue(Double.isNaN(meter.perSecond()));
    meter.sample(0, 100);
    assertTrue(Double.isNaN(meter.perSecond()));
  }

  @Test
  void unknownWithoutElapsedTime() {
    RateMeter meter = new RateMeter(1000 * MS, 8);
    meter.sample(5, 100);
    meter.sample(5, 200);
    assertTrue(Double.isNaN(meter.perSecond()));
  }

  @Test
  void usesOldestSampleWhileHistoryIsShorterThanWindow() {
    RateMeter meter = new RateMeter(1000 * MS, 64);
    meter.sample(0, 0);
    meter.sample(250 * MS, 100);
    // 100 units in a quarter of a second.
    assertEquals(400, meter.perSecond(), 1e-9);
  }

  @Test
  void averagesOverNewestFullWindow() {
    RateMeter meter = new RateMeter(1000 * MS, 64);
    // 50 ms ticks: a burst of 1000 at the start, then 10 per tick.
    meter.sample(0, 0);
    meter.sample(50 * MS, 1000);
    long total = 1000;
    for (long t = 100; t <= 2000; t += 50) {
      total += 10;
      meter.sample(t * MS, total);
    }
    // From 1000 ms to 2000 ms: 20 ticks of 10.
    assertEquals(200, meter.perSecond(), 1e-9);
  }

  @Test
  void keepsWorkingAfterWrapping() {
    RateMeter meter = new RateMeter(100 * MS, 4);
    long total = 0;
    for (long t = 0; t <= 1000; t += 50) {
      meter.sample(t * MS, total);
      total += 5;
    }
    // 5 units per 50 ms.
    assertEquals(100, meter.perSecond(), 1e-9);
  }
}
