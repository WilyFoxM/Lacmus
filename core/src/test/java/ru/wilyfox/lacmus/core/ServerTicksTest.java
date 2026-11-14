package ru.wilyfox.lacmus.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ServerTicksTest {
  private static final long MS = 1_000_000L;

  /** Time packets every {@code everyMs} with 20 ticks between them, from {@code fromMs}. */
  private static long feed(ServerTicks ticks, long gameTime, long fromMs, long everyMs, int count) {
    long t = fromMs;
    for (int i = 0; i < count; i++) {
      ticks.timeSync(gameTime, t * MS);
      ticks.update(t * MS);
      gameTime += ServerTicks.SYNC_TICKS;
      t += everyMs;
    }
    return gameTime;
  }

  @Test
  void unknownBeforeTwoPackets() {
    ServerTicks ticks = new ServerTicks();
    ticks.update(0);
    assertTrue(Double.isNaN(ticks.ticksPerSecond()));
    assertTrue(Double.isNaN(ticks.lateMillis()));
    feed(ticks, 100, 0, 1000, 1);
    assertTrue(Double.isNaN(ticks.ticksPerSecond()));
  }

  @Test
  void healthyServerTicksTwentyTimesASecond() {
    ServerTicks ticks = new ServerTicks();
    feed(ticks, 0, 0, 1000, 8);
    assertEquals(20, ticks.ticksPerSecond(), 1e-9);
    assertEquals(20, ticks.targetTickRate());
    assertFalse(ticks.frozen());
  }

  @Test
  void slowServerShowsFewerTicks() {
    ServerTicks ticks = new ServerTicks();
    // 20 ticks every 1.25 s: 16 ticks a second.
    feed(ticks, 0, 0, 1250, 8);
    assertEquals(16, ticks.ticksPerSecond(), 1e-9);
  }

  @Test
  void jitterOnSingleArrivalsAveragesOut() {
    ServerTicks ticks = new ServerTicks();
    // Single arrivals off by up to 80 ms.
    long[] arrivals = {0, 1080, 1960, 3050, 3990, 5020, 6000};
    for (int i = 0; i < arrivals.length; i++) {
      ticks.timeSync(i * 20L, arrivals[i] * MS);
      ticks.update(arrivals[i] * MS);
    }
    // Single arrivals 80 ms off move a 5 s estimate by under 2 %.
    assertEquals(20, ticks.ticksPerSecond(), 0.4);
  }

  @Test
  void frozenServerKeepsSendingTheSameTime() {
    ServerTicks ticks = new ServerTicks();
    ticks.tickingState(20, true);
    for (int i = 0; i < 6; i++) {
      ticks.timeSync(500, i * 1000 * MS);
      ticks.update(i * 1000 * MS);
    }
    assertTrue(ticks.frozen());
    assertEquals(0, ticks.ticksPerSecond(), 1e-9);
  }

  @Test
  void tickRateCommandChangesTargetAndInterval() {
    ServerTicks ticks = new ServerTicks();
    ticks.tickingState(10, false);
    assertEquals(10, ticks.targetTickRate());
    assertEquals(2_000 * MS, ticks.syncIntervalNanos());
    feed(ticks, 0, 0, 2000, 6);
    assertEquals(10, ticks.ticksPerSecond(), 1e-9);
  }

  @Test
  void lateTimePacketIsReported() {
    ServerTicks ticks = new ServerTicks();
    feed(ticks, 0, 0, 1000, 3);
    // Last packet at 2000 ms; the next is due at 3000 ms.
    ticks.update(3_400 * MS);
    assertTrue(Double.isNaN(ticks.lateMillis()), "within half an interval");
    ticks.update(4_200 * MS);
    assertEquals(1_200, ticks.lateMillis(), 1e-9);
    ticks.timeSync(60, 4_300 * MS);
    ticks.update(4_300 * MS);
    assertTrue(Double.isNaN(ticks.lateMillis()));
  }

  @Test
  void pairIsReadConsistently() throws InterruptedException {
    PublishedPair pair = new PublishedPair();
    assertEquals(0, pair.read());
    int writes = 200_000;
    Thread writer =
        new Thread(
            () -> {
              for (long i = 1; i <= writes; i++) {
                pair.set(i, -i);
              }
            });
    writer.start();
    long lastVersion = 0;
    while (writer.isAlive() || lastVersion == 0) {
      long version = pair.read();
      assertEquals(pair.first(), -pair.second(), "both values from one write");
      assertTrue(version >= lastVersion);
      lastVersion = version;
    }
    writer.join();
    pair.read();
    assertEquals(writes, pair.first());
  }

  /** Feeds one packet and returns the estimate after it. */
  private static double at(ServerTicks ticks, long gameTime, long arrivalMs) {
    ticks.timeSync(gameTime, arrivalMs * MS);
    ticks.update(arrivalMs * MS);
    return ticks.ticksPerSecond();
  }

  @Test
  void heldUpPacketsAfterJoiningDoNotOvershoot() {
    // Like a proxy switching servers: the packets sent at 0, 1 and 2 s are held until 2.2 s,
    // then the rest arrive on time.
    ServerTicks ticks = new ServerTicks();
    at(ticks, 0, 2_200);
    at(ticks, 20, 2_200);
    at(ticks, 40, 2_200);
    for (int i = 3; i <= 12; i++) {
      double estimate = at(ticks, i * 20L, i * 1000L);
      int second = i;
      assertTrue(
          Double.isNaN(estimate) || Math.abs(estimate - 20) < 0.01,
          () -> "at " + second + " s: " + estimate);
    }
    assertEquals(20, ticks.ticksPerSecond(), 0.01);
  }

  @Test
  void oneLatePacketIsNeverTheStart() {
    ServerTicks ticks = new ServerTicks();
    for (int i = 0; i <= 12; i++) {
      long arrival = i * 1000L + (i == 4 ? 400 : 0);
      double estimate = at(ticks, i * 20L, arrival);
      int second = i;
      if (i != 4 && !Double.isNaN(estimate)) {
        assertEquals(20, estimate, 0.01, () -> "at " + second + " s");
      }
    }
  }

  @Test
  void aLateNewestPacketPullsTheEstimateDown() {
    ServerTicks ticks = new ServerTicks();
    for (int i = 0; i <= 5; i++) {
      at(ticks, i * 20L, i * 1000L);
    }
    // The server stalls and skips ticks: the next time packet comes 4 s late and only 20 ticks on.
    double estimate = at(ticks, 120, 10_000);
    assertTrue(estimate < 18, "lag shows: " + estimate);
  }

  @Test
  void gameTimeGoingBackStartsOver() {
    ServerTicks ticks = new ServerTicks();
    for (int i = 0; i <= 5; i++) {
      at(ticks, 10_000 + i * 20L, i * 1000L);
    }
    assertEquals(20, ticks.ticksPerSecond(), 0.01);
    // Another server with an earlier game time.
    assertTrue(Double.isNaN(at(ticks, 500, 6_000)));
    at(ticks, 520, 7_000);
    at(ticks, 540, 8_000);
    assertEquals(20, at(ticks, 560, 9_000), 0.01);
  }

  @Test
  void sprintingServerShowsItsSpeed() {
    ServerTicks ticks = new ServerTicks();
    // 20 ticks every 0.25 s: 80 ticks a second.
    for (int i = 0; i <= 20; i++) {
      at(ticks, i * 20L, i * 250L);
    }
    assertEquals(80, ticks.ticksPerSecond(), 0.01);
  }

  @Test
  void slightlyHeldUpStartLiftsTheFirstEstimateByLittle() {
    // The packet sent at 0 s arrives 45 ms late, within the jitter allowance.
    ServerTicks ticks = new ServerTicks();
    at(ticks, 0, 45);
    for (int i = 1; i <= 8; i++) {
      double estimate = at(ticks, i * 20L, i * 1000L);
      int second = i;
      assertTrue(
          Double.isNaN(estimate) || estimate <= 20 * 1.017,
          () -> "at " + second + " s: " + estimate);
    }
  }

  @Test
  void slowTickRateWithJitterStillGetsAnEstimate() {
    ServerTicks ticks = new ServerTicks();
    ticks.tickingState(10, false);
    long[] jitter = {0, 30, -20, 10, 40, -30, 20, 0, -10, 30, 10, -20};
    for (int i = 0; i < jitter.length; i++) {
      at(ticks, i * 20L, i * 2000L + 100 + jitter[i]);
    }
    assertEquals(10, ticks.ticksPerSecond(), 0.15);
  }
}
