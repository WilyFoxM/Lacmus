package ru.wilyfox.lacmus.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.management.ManagementFactory;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class HudSignatureTest {
  private static NetView view(double rtt, double inBytes, int fps, boolean alarm) {
    return new NetView(
        fps,
        NetView.Rtt.VALUE,
        rtt,
        rtt,
        3.2,
        Double.NaN,
        Double.NaN,
        NetView.Tick.ESTIMATE,
        19.94,
        20,
        Double.NaN,
        4.2,
        18.3,
        2.1,
        true,
        inBytes,
        1_234,
        40,
        20,
        alarm,
        false,
        false,
        false,
        null,
        false);
  }

  private static long[] signature(NetView view, HudSettings settings) {
    long[] out = new long[HudSignature.LENGTH];
    HudSignature.fill(view, settings, out);
    return out;
  }

  @Test
  void changesBelowWhatIsShownKeepTheSignature() {
    HudSettings settings = new HudSettings();
    assertArrayEquals(
        signature(view(42.1, 12_340, 144, false), settings),
        signature(view(42.4, 12_360, 144, false), settings));
  }

  @Test
  void anythingShownChangesTheSignature() {
    HudSettings settings = new HudSettings();
    long[] base = signature(view(42.1, 12_340, 144, false), settings);
    assertFalse(Arrays.equals(base, signature(view(42.6, 12_340, 144, false), settings)));
    assertFalse(Arrays.equals(base, signature(view(42.1, 13_600, 144, false), settings)));
    assertFalse(Arrays.equals(base, signature(view(42.1, 12_340, 143, false), settings)));
    assertFalse(Arrays.equals(base, signature(view(42.1, 12_340, 144, true), settings)));
    HudSettings easy = new HudSettings();
    easy.mode = HudSettings.Mode.EASY;
    assertFalse(Arrays.equals(base, signature(view(42.1, 12_340, 144, false), easy)));
    HudSettings noJitter = new HudSettings();
    noJitter.showJitter = false;
    assertFalse(Arrays.equals(base, signature(view(42.1, 12_340, 144, false), noJitter)));
  }

  @Test
  void fillingDoesNotAllocate() {
    var threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
    HudSettings settings = new HudSettings();
    NetView view = view(42.1, 12_340, 144, false);
    long[] out = new long[HudSignature.LENGTH];
    for (int i = 0; i < 20_000; i++) {
      HudSignature.fill(view, settings, out);
    }
    long before = threads.getCurrentThreadAllocatedBytes();
    for (int i = 0; i < 1_000; i++) {
      HudSignature.fill(view, settings, out);
    }
    long allocated = threads.getCurrentThreadAllocatedBytes() - before;
    assertEquals(0, allocated, "bytes allocated by 1000 signatures");
  }

  @Test
  void serverReportChangesTheSignature() {
    HudSettings settings = new HudSettings();
    NetView base = view(42.1, 12_340, 144, false);
    NetView reported =
        new NetView(
            base.fps(),
            base.rtt(),
            base.rttMillis(),
            base.pingMillis(),
            base.jitterMillis(),
            base.stallMillis(),
            base.timeoutInMillis(),
            base.tick(),
            base.ticksPerSecond(),
            base.targetTickRate(),
            base.tickLateMillis(),
            base.queueMillis(),
            base.frameMaxMillis(),
            base.clientTickMillis(),
            base.bytesKnown(),
            base.inboundBytesPerSecond(),
            base.outboundBytesPerSecond(),
            base.inboundPacketsPerSecond(),
            base.outboundPacketsPerSecond(),
            base.connectionAlarm(),
            base.tickAlarm(),
            base.queueAlarm(),
            base.clientTickAlarm(),
            new ServerReport(20, false, 20, 12.2, 31),
            false);
    assertFalse(Arrays.equals(signature(base, settings), signature(reported, settings)));
  }
}
