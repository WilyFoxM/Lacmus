package ru.wilyfox.lacmus.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HudSettingsTest {
  @Test
  void defaults() {
    HudSettings s = new HudSettings();
    assertTrue(s.visible);
    assertEquals(HudSettings.Mode.FULL, s.mode);
    assertEquals(HudSettings.Latency.RTT, s.latency);
    assertEquals(250, s.queueAlarmMs);
    assertEquals(50, s.clientTickAlarmMs);
    assertEquals(90, s.tpsAlarmPercent);
    assertEquals(500_000_000L, s.pingIntervalNanos());
  }

  @Test
  void sanitizeClampsAndFillsMissingChoices() {
    HudSettings s = new HudSettings();
    s.mode = null;
    s.latency = null;
    s.anchor = null;
    s.scale = 5;
    s.panelOpacity = 300;
    s.queueAlarmMs = 0;
    s.clientTickAlarmMs = 10_000;
    s.tpsAlarmPercent = 10;
    s.pingsPerSecond = 50;
    s.sanitize();
    assertEquals(HudSettings.Mode.FULL, s.mode);
    assertEquals(HudSettings.Latency.RTT, s.latency);
    assertEquals(HudSettings.Anchor.TOP_LEFT, s.anchor);
    assertEquals(HudSettings.MIN_SCALE, s.scale);
    assertEquals(100, s.panelOpacity);
    assertEquals(HudSettings.MIN_QUEUE_ALARM_MS, s.queueAlarmMs);
    assertEquals(HudSettings.MAX_CLIENT_TICK_ALARM_MS, s.clientTickAlarmMs);
    assertEquals(HudSettings.MIN_TPS_ALARM_PERCENT, s.tpsAlarmPercent);
    assertEquals(HudSettings.MAX_PINGS_PER_SECOND, s.pingsPerSecond);
  }

  @Test
  void timedSamplesMeanOverTheWindow() {
    TimedSamples samples = new TimedSamples(4);
    assertTrue(Double.isNaN(samples.meanSince(0)));
    samples.add(1, 10);
    samples.add(2, 20);
    samples.add(3, 30);
    assertEquals(25, samples.meanSince(2), 1e-9);
    samples.add(4, 40);
    samples.add(5, 50);
    // Capacity 4: the sample at 1 is gone.
    assertEquals(35, samples.meanSince(0), 1e-9);
    samples.clear();
    assertTrue(Double.isNaN(samples.meanSince(0)));
  }
}
