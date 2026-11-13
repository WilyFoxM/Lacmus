package ru.wilyfox.lacmus.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NetProbeTest {
  private static final long MS = 1_000_000L;

  @Test
  void ratesFollowCounts() {
    NetProbe probe = new NetProbe();
    probe.update(0);
    for (int tick = 1; tick <= 20; tick++) {
      probe.inboundBytes(500);
      probe.outboundBytes(50);
      probe.inboundPacket(tick * 50 * MS - 20 * MS);
      probe.inboundPacket(tick * 50 * MS - 10 * MS);
      probe.outboundPacket();
      probe.update(tick * 50 * MS);
    }
    assertEquals(10_000, probe.inboundBytesPerSecond(), 1e-6);
    assertEquals(1_000, probe.outboundBytesPerSecond(), 1e-6);
    assertEquals(40, probe.inboundPacketsPerSecond(), 1e-6);
    assertEquals(20, probe.outboundPacketsPerSecond(), 1e-6);
  }

  @Test
  void roundTripIsMeasuredBetweenNetworkStamps() {
    NetProbe probe = new NetProbe();
    probe.pingSent(7, 1_000 * MS);
    probe.pongReceived(7, 1_042 * MS);
    // The client thread picks it up much later; the trip is still 42 ms.
    probe.update(1_300 * MS);
    assertEquals(42, probe.rttMillis(), 1e-9);
    assertTrue(Double.isNaN(probe.jitterMillis()));
  }

  @Test
  void jitterAndStaleness() {
    NetProbe probe = new NetProbe();
    long t = 0;
    for (long trip : new long[] {40, 50, 40}) {
      probe.pingSent(t, t * MS);
      probe.pongReceived(t, (t + trip) * MS);
      t += 500;
      probe.update(t * MS);
    }
    assertEquals(40, probe.rttMillis(), 1e-9);
    assertEquals(10, probe.jitterMillis(), 1e-9);

    probe.update((t + NetProbe.RTT_STALE_NANOS / MS + 1) * MS);
    assertTrue(Double.isNaN(probe.rttMillis()));
    assertTrue(Double.isNaN(probe.jitterMillis()));
  }

  @Test
  void tripsAfterLongGapStartAFreshWindow() {
    NetProbe probe = new NetProbe();
    probe.pingSent(1, 0);
    probe.pongReceived(1, 100 * MS);
    probe.update(100 * MS);
    long later = 100 * MS + NetProbe.RTT_STALE_NANOS + MS;
    probe.pingSent(2, later);
    probe.pongReceived(2, later + 20 * MS);
    probe.update(later + 20 * MS);
    assertEquals(20, probe.rttMillis(), 1e-9);
    // The 100 ms trip before the gap is not mixed into the jitter.
    assertTrue(Double.isNaN(probe.jitterMillis()));
  }

  @Test
  void unknownPongIsIgnored() {
    NetProbe probe = new NetProbe();
    probe.pongReceived(5, 10 * MS);
    probe.update(10 * MS);
    assertTrue(Double.isNaN(probe.rttMillis()));
  }

  @Test
  void noStallBeforeFirstPacket() {
    NetProbe probe = new NetProbe();
    probe.update(60_000 * MS);
    assertTrue(Double.isNaN(probe.stallMillis()));
    assertTrue(Double.isNaN(probe.timeoutInMillis()));
  }

  @Test
  void shortSilenceIsNotAStall() {
    NetProbe probe = new NetProbe();
    probe.inboundPacket(1_000 * MS);
    probe.update(1_000 * MS + NetProbe.STALL_NANOS - MS);
    assertTrue(Double.isNaN(probe.stallMillis()));
  }

  @Test
  void stallGrowsAndWarnsBeforeTimeout() {
    NetProbe probe = new NetProbe();
    probe.inboundPacket(1_000 * MS);
    probe.update(3_400 * MS);
    assertEquals(2_400, probe.stallMillis(), 1e-9);
    assertTrue(Double.isNaN(probe.timeoutInMillis()));

    probe.update(9_000 * MS);
    assertEquals(8_000, probe.stallMillis(), 1e-9);
    assertEquals(22_000, probe.timeoutInMillis(), 1e-9);

    probe.update(40_000 * MS);
    assertEquals(0, probe.timeoutInMillis(), 1e-9);
  }

  @Test
  void packetEndsTheStall() {
    NetProbe probe = new NetProbe();
    probe.inboundPacket(0);
    probe.update(3_000 * MS);
    assertEquals(3_000, probe.stallMillis(), 1e-9);
    probe.inboundPacket(3_100 * MS);
    probe.update(3_200 * MS);
    assertTrue(Double.isNaN(probe.stallMillis()));
  }

  @Test
  void packetAfterTheClockReadingIsNoSilence() {
    NetProbe probe = new NetProbe();
    probe.inboundPacket(5_000 * MS);
    probe.update(4_999 * MS);
    assertTrue(Double.isNaN(probe.stallMillis()));
  }
}
