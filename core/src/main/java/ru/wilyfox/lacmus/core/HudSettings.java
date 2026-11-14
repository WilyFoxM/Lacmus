package ru.wilyfox.lacmus.core;

/**
 * The player's settings, with bounded values. Plain fields, so a JSON library can read and write
 * them; {@link #sanitize()} brings anything read from a file back into range. A field missing from
 * a saved file keeps its default.
 */
public final class HudSettings {
  /** How much the HUD shows. */
  public enum Mode {
    /** One line: frame rate, latency and jitter. */
    EASY,
    /** Every metric, one line per side of the game. */
    FULL
  }

  /** Which latency the HUD shows. */
  public enum Latency {
    /** {@code rtt}: the newest round trip. */
    RTT,
    /** {@code ping}: the mean round trip of the last {@link NetProbe#PING_WINDOW_NANOS}. */
    PING
  }

  /** The screen corner the HUD sits in. */
  public enum Anchor {
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
  }

  public static final int SCHEMA_VERSION = 1;

  public static final int MIN_SCALE = 50;
  public static final int MAX_SCALE = 200;
  public static final int MIN_QUEUE_ALARM_MS = 50;
  public static final int MAX_QUEUE_ALARM_MS = 2000;
  public static final int MIN_CLIENT_TICK_ALARM_MS = 20;
  public static final int MAX_CLIENT_TICK_ALARM_MS = 200;
  public static final int MIN_TPS_ALARM_PERCENT = 50;
  public static final int MAX_TPS_ALARM_PERCENT = 100;
  public static final int MIN_PINGS_PER_SECOND = 1;
  public static final int MAX_PINGS_PER_SECOND = 5;

  public int schemaVersion = SCHEMA_VERSION;

  // General.
  public boolean visible = true;
  public Mode mode = Mode.FULL;
  public Latency latency = Latency.RTT;
  public Anchor anchor = Anchor.TOP_LEFT;

  /** Size of the HUD text, in percent of the game's interface scale. */
  public int scale = 100;

  /** Opacity of the panel behind the HUD, in percent. */
  public int panelOpacity = 75;

  // Parts shown in the full mode.
  public boolean showFrameMax = true;
  public boolean showClientTick = true;
  public boolean showQueue = true;
  public boolean showJitter = true;
  public boolean showBytes = true;
  public boolean showPackets = true;
  public boolean showServer = true;

  // Alarms.
  public boolean alarms = true;
  public int queueAlarmMs = 250;
  public int clientTickAlarmMs = 50;

  /** The server line alarms below this share of the server's target tick rate. */
  public int tpsAlarmPercent = 90;

  // Network.

  /** Lacmus sends the game's own ping requests while the HUD is shown on a server. */
  public boolean pingRequests = true;

  public int pingsPerSecond = 2;

  /** Brings every value back into its range and replaces missing choices with defaults. */
  public void sanitize() {
    HudSettings defaults = new HudSettings();
    if (mode == null) {
      mode = defaults.mode;
    }
    if (latency == null) {
      latency = defaults.latency;
    }
    if (anchor == null) {
      anchor = defaults.anchor;
    }
    scale = Math.clamp(scale, MIN_SCALE, MAX_SCALE);
    panelOpacity = Math.clamp(panelOpacity, 0, 100);
    queueAlarmMs = Math.clamp(queueAlarmMs, MIN_QUEUE_ALARM_MS, MAX_QUEUE_ALARM_MS);
    clientTickAlarmMs =
        Math.clamp(clientTickAlarmMs, MIN_CLIENT_TICK_ALARM_MS, MAX_CLIENT_TICK_ALARM_MS);
    tpsAlarmPercent = Math.clamp(tpsAlarmPercent, MIN_TPS_ALARM_PERCENT, MAX_TPS_ALARM_PERCENT);
    pingsPerSecond = Math.clamp(pingsPerSecond, MIN_PINGS_PER_SECOND, MAX_PINGS_PER_SECOND);
  }

  /** Time between the HUD's own ping requests. */
  public long pingIntervalNanos() {
    return 1_000_000_000L / Math.clamp(pingsPerSecond, MIN_PINGS_PER_SECOND, MAX_PINGS_PER_SECOND);
  }
}
