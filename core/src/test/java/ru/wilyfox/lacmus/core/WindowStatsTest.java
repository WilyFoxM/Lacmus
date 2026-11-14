package ru.wilyfox.lacmus.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WindowStatsTest {
  private static final long MS = 1_000_000L;

  @Test
  void emptyIsUnknown() {
    WindowStats stats = new WindowStats(4);
    assertTrue(Double.isNaN(stats.meanMillis()));
    stats.closeTick();
    assertTrue(Double.isNaN(stats.maxMillis()));
  }

  @Test
  void meanAndMaxAcrossTicks() {
    WindowStats stats = new WindowStats(4);
    // Three frames in one tick, one long frame in the next.
    stats.add(5 * MS);
    stats.add(6 * MS);
    stats.add(7 * MS);
    stats.closeTick();
    stats.add(30 * MS);
    stats.closeTick();
    assertEquals(12, stats.meanMillis(), 1e-9);
    assertEquals(30, stats.maxMillis(), 1e-9);
  }

  @Test
  void oldTicksLeaveTheWindow() {
    WindowStats stats = new WindowStats(2);
    stats.add(100 * MS);
    stats.closeTick();
    stats.add(4 * MS);
    stats.closeTick();
    stats.add(6 * MS);
    stats.closeTick();
    assertEquals(5, stats.meanMillis(), 1e-9);
    assertEquals(6, stats.maxMillis(), 1e-9);
  }

  @Test
  void ticksWithoutValuesDoNotCountAsZero() {
    WindowStats stats = new WindowStats(4);
    stats.add(8 * MS);
    stats.closeTick();
    stats.closeTick();
    assertEquals(8, stats.meanMillis(), 1e-9);
  }

  @Test
  void clearForgets() {
    WindowStats stats = new WindowStats(4);
    stats.add(8 * MS);
    stats.closeTick();
    stats.clear();
    assertTrue(Double.isNaN(stats.meanMillis()));
  }
}
