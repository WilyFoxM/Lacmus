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
    // The window reaches back to the packet at 0 ms: 120 ticks in 6 s, the jitter cancels out.
    assertEquals(20, ticks.ticksPerSecond(), 1e-9);
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
}
