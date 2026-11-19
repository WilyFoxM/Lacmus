package ru.wilyfox.lacmus.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ServerReportTest {
  private static final long SECOND = 1_000_000_000L;

  @Test
  void roundTrips() {
    ServerReport report = new ServerReport(20, false, 19.96, 12.5, 31.25);
    ServerReport back = ServerReport.decode(report.encode());
    assertEquals(20, back.targetTickRate());
    assertEquals(false, back.frozen());
    assertEquals(19.96, back.ticksPerSecond(), 1e-4);
    assertEquals(12.5, back.msptMean(), 1e-4);
    assertEquals(31.25, back.msptMax(), 1e-4);
  }

  @Test
  void keepsFrozenAndUnknowns() {
    ServerReport back =
        ServerReport.decode(new ServerReport(10, true, Double.NaN, Double.NaN, 3).encode());
    assertTrue(back.frozen());
    assertEquals(10, back.targetTickRate());
    assertTrue(Double.isNaN(back.ticksPerSecond()));
    assertTrue(Double.isNaN(back.msptMean()));
  }

  @Test
  void ignoresOtherVersionsAndBrokenPayloads() {
    byte[] bytes = new ServerReport(20, false, 20, 5, 6).encode();
    bytes[0] = 2;
    assertNull(ServerReport.decode(bytes));
    assertNull(ServerReport.decode(new byte[] {1, 0, 0}));
    assertNull(ServerReport.decode(null));
    assertNull(ServerReport.decode(new ServerReport(0, false, 20, 5, 6).encode()));
  }

  @Test
  void budgetFollowsTheTargetRate() {
    assertEquals(50, new ServerReport(20, false, 20, 1, 1).tickBudgetMillis(), 1e-9);
    assertEquals(100, new ServerReport(10, false, 10, 1, 1).tickBudgetMillis(), 1e-9);
  }

  @Test
  void tickLogNeedsTheWholeWindowForTheRate() {
    ServerTickLog log = new ServerTickLog();
    long t = 100 * SECOND;
    for (int i = 0; i < 60; i++) {
      t += SECOND / 20;
      log.tick(t, 10_000_000);
    }
    assertTrue(Double.isNaN(log.ticksPerSecond(t)), "three seconds are not enough");
    for (int i = 0; i < 60; i++) {
      t += SECOND / 20;
      log.tick(t, 10_000_000);
    }
    assertEquals(20, log.ticksPerSecond(t), 0.01);
  }

  @Test
  void tickLogSeesASlowServer() {
    ServerTickLog log = new ServerTickLog();
    long t = 0;
    for (int i = 0; i < 100; i++) {
      t += SECOND / 12;
      log.tick(t, 80_000_000);
    }
    assertEquals(12, log.ticksPerSecond(t), 0.01);
    assertEquals(80, log.msptMean(), 1e-9);
  }

  @Test
  void durationsAreTheLastTwentyTicks() {
    ServerTickLog log = new ServerTickLog();
    long t = 0;
    for (int i = 0; i < 40; i++) {
      t += SECOND / 20;
      log.tick(t, i < 20 ? 40_000_000 : 10_000_000);
    }
    log.tick(t + SECOND / 20, 70_000_000);
    assertEquals((19 * 10 + 70) / 20.0, log.msptMean(), 1e-9);
    assertEquals(70, log.msptMax(), 1e-9);
  }

  @Test
  void fastTicksFillTheRing() {
    ServerTickLog log = new ServerTickLog();
    long t = 0;
    for (int i = 0; i < 5000; i++) {
      t += SECOND / 500;
      log.tick(t, 1_000_000);
    }
    assertEquals(500, log.ticksPerSecond(t), 0.5);
  }

  @Test
  void emptyLogKnowsNothing() {
    ServerTickLog log = new ServerTickLog();
    assertTrue(Double.isNaN(log.msptMean()));
    assertTrue(Double.isNaN(log.msptMax()));
    assertTrue(Double.isNaN(log.ticksPerSecond(SECOND)));
  }

  @Test
  void reportsGoStale() {
    ServerReports reports = new ServerReports();
    reports.accept(new ServerReport(20, false, 20, 5, 6).encode(), 10 * SECOND);
    assertEquals(20, reports.current(12 * SECOND).targetTickRate());
    assertNull(reports.current(14 * SECOND));
    reports.accept(new byte[] {9}, 14 * SECOND);
    assertNull(reports.current(14 * SECOND), "an unreadable payload does not refresh");
    reports.clear();
    assertNull(reports.current(10 * SECOND));
  }

  @Test
  void stoppedServerReportsZeroOnceTheWindowIsEmpty() {
    ServerTickLog log = new ServerTickLog();
    long t = 0;
    for (int i = 0; i < 200; i++) {
      t += SECOND / 20;
      log.tick(t, 1_000_000);
    }
    assertEquals(0, log.ticksPerSecond(t + 6 * SECOND), 1e-9);
  }
}
