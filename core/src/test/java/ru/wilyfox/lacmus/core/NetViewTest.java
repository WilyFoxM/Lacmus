package ru.wilyfox.lacmus.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import ru.wilyfox.lacmus.core.NetView.Rtt;
import ru.wilyfox.lacmus.core.NetView.Tick;

class NetViewTest {
  private static final long MS = 1_000_000L;

  /** A server ticking at {@code tps}, with pongs, up to {@code untilMs}. */
  private static NetProbe healthy(double tps, long untilMs) {
    NetProbe probe = new NetProbe();
    long gameTime = 0;
    for (long t = 0; t <= untilMs; t += 500) {
      probe.pingSent(t, t * MS);
      probe.pongReceived(t, (t + 40) * MS);
      probe.inboundPacket((t + 40) * MS);
      if (t % 1000 == 0) {
        probe.timeSync(gameTime, t * MS);
        gameTime += Math.round(tps);
      }
      probe.update((t + 40) * MS);
    }
    return probe;
  }

  @Test
  void healthyServer() {
    NetView view = NetView.of(healthy(20, 6000), new ClientStats(), 144, false, false);
    assertEquals(Rtt.VALUE, view.rtt());
    assertEquals(40, view.rttMillis(), 1e-9);
    assertEquals(Tick.ESTIMATE, view.tick());
    assertEquals(20, view.ticksPerSecond(), 1e-9);
    assertTrue(view.bytesKnown());
    assertFalse(view.connectionAlarm());
    assertFalse(view.tickAlarm());
  }

  @Test
  void laggingServerRaisesTheTickAlarm() {
    NetView view = NetView.of(healthy(15, 6000), new ClientStats(), 144, false, false);
    assertEquals(Tick.ESTIMATE, view.tick());
    assertEquals(15, view.ticksPerSecond(), 1e-9);
    assertTrue(view.tickAlarm());
    assertFalse(view.connectionAlarm());
  }

  @Test
  void stallOnAServerReplacesTheRoundTripAndHidesTicks() {
    NetProbe probe = healthy(20, 6000);
    probe.update(9_000 * MS);
    NetView view = NetView.of(probe, new ClientStats(), 144, false, false);
    assertEquals(Rtt.STALL, view.rtt());
    assertTrue(Double.isNaN(view.rttMillis()));
    assertEquals(9_000 - 6_040, view.stallMillis(), 1e-9);
    assertEquals(Tick.UNKNOWN, view.tick(), "the connection, not the server, is stuck");
    assertTrue(view.connectionAlarm());
    assertFalse(view.tickAlarm());
  }

  @Test
  void busyServerKeepsAnsweringPings() {
    // Time packets stop (the server thread is busy), pongs keep coming from its network thread.
    NetProbe probe = new NetProbe();
    long gameTime = 0;
    for (long t = 0; t <= 9000; t += 500) {
      probe.pingSent(t, t * MS);
      probe.pongReceived(t, (t + 40) * MS);
      probe.inboundPacket((t + 40) * MS);
      if (t % 1000 == 0 && t <= 5000) {
        probe.timeSync(gameTime, t * MS);
        gameTime += 20;
      }
      probe.update((t + 40) * MS);
    }
    NetView view = NetView.of(probe, new ClientStats(), 144, false, false);
    assertEquals(Rtt.VALUE, view.rtt());
    assertFalse(view.connectionAlarm());
    assertEquals(Tick.LATE, view.tick());
    assertEquals(9_040 - 5_000 - 1_000, view.tickLateMillis(), 1e-9);
    assertTrue(view.tickAlarm());
  }

  @Test
  void frozenServerIsNotAnAlarm() {
    NetProbe probe = healthy(20, 6000);
    probe.tickingState(20, true);
    NetView view = NetView.of(probe, new ClientStats(), 144, false, false);
    assertEquals(Tick.FROZEN, view.tick());
    assertFalse(view.tickAlarm());
  }

  @Test
  void slowTargetTickRateIsNotAnAlarm() {
    NetProbe probe = new NetProbe();
    probe.tickingState(10, false);
    long gameTime = 0;
    for (long t = 0; t <= 12_000; t += 500) {
      probe.inboundPacket(t * MS);
      if (t % 2000 == 0) {
        probe.timeSync(gameTime, t * MS);
        gameTime += 20;
      }
      probe.update(t * MS);
    }
    NetView view = NetView.of(probe, new ClientStats(), 144, false, false);
    assertEquals(Tick.ESTIMATE, view.tick());
    assertEquals(10, view.ticksPerSecond(), 1e-9);
    assertEquals(10, view.targetTickRate());
    assertFalse(view.tickAlarm());
  }

  @Test
  void singlePlayerHasNoRoundTripAndShowsPauseAsNoData() {
    NetProbe probe = healthy(20, 6000);
    probe.update(9_000 * MS);
    NetView view = NetView.of(probe, new ClientStats(), 60, true, false);
    assertEquals(Rtt.NONE, view.rtt());
    assertFalse(view.bytesKnown());
    assertEquals(9_000 - 6_040, view.stallMillis(), 1e-9);
    assertTrue(Double.isNaN(view.timeoutInMillis()), "no read timeout in memory");
    assertTrue(view.connectionAlarm());
    assertEquals(Tick.LATE, view.tick(), "the integrated server stopped ticking");
  }

  @Test
  void pausedSinglePlayerIsQuiet() {
    NetProbe probe = healthy(20, 6000);
    probe.update(30_000 * MS);
    NetView view = NetView.of(probe, new ClientStats(), 60, true, true);
    assertTrue(Double.isNaN(view.stallMillis()));
    assertEquals(Tick.PAUSED, view.tick());
    assertFalse(view.connectionAlarm());
    assertFalse(view.tickAlarm());
  }

  @Test
  void pauseScreenOnAServerChangesNothing() {
    NetProbe probe = healthy(20, 6000);
    probe.update(9_000 * MS);
    NetView view = NetView.of(probe, new ClientStats(), 60, false, true);
    assertEquals(Rtt.STALL, view.rtt());
    assertTrue(view.connectionAlarm());
  }

  @Test
  void slowQueueRaisesTheClientAlarm() {
    ClientStats client = new ClientStats();
    Object item = new Object();
    client.queue().queued(item, 0);
    client.queue().handled(item, 300 * MS);
    client.update();
    NetView view = NetView.of(healthy(20, 6000), client, 144, false, false);
    assertEquals(300, view.queueMillis(), 1e-9);
    assertTrue(view.queueAlarm());
    assertTrue(view.clientAlarm());
    assertFalse(view.connectionAlarm());
    assertFalse(view.tickAlarm());
  }

  @Test
  void quickQueueIsFine() {
    ClientStats client = new ClientStats();
    Object item = new Object();
    client.queue().queued(item, 0);
    client.queue().handled(item, 6 * MS);
    client.update();
    NetView view = NetView.of(healthy(20, 6000), client, 144, false, false);
    assertEquals(6, view.queueMillis(), 1e-9);
    assertFalse(view.clientAlarm());
  }

  @Test
  void framesAndClientTicks() {
    ClientStats client = new ClientStats();
    for (int tick = 0; tick < 20; tick++) {
      client.frames().add(7 * MS);
      client.frames().add(tick == 5 ? 40 * MS : 7 * MS);
      client.ticks().add(2 * MS);
      client.update();
    }
    NetView view = NetView.of(healthy(20, 6000), client, 144, false, false);
    assertEquals(40, view.frameMaxMillis(), 1e-9);
    assertEquals(2, view.clientTickMillis(), 1e-9);
    assertFalse(view.clientAlarm());
  }

  @Test
  void slowClientTicksRaiseTheClientAlarm() {
    ClientStats client = new ClientStats();
    for (int tick = 0; tick < 20; tick++) {
      client.ticks().add(60 * MS);
      client.update();
    }
    NetView view = NetView.of(healthy(20, 6000), client, 15, false, false);
    assertTrue(view.clientTickAlarm());
    assertTrue(view.clientAlarm());
  }
}
