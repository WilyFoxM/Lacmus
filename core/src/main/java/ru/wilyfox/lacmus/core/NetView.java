package ru.wilyfox.lacmus.core;

/**
 * What the HUD shows, decided from the probe and the client's state. Numbers are raw (NaN when
 * unknown); formatting and words are left to the HUD.
 *
 * <p>Rules:
 *
 * <ul>
 *   <li>Single player has no round trip and no bytes on a wire. While the game is paused there, the
 *       integrated server is paused too, so silence and missing ticks are expected, not a problem.
 *   <li>On a server, a stall replaces the round trip, and the server's ticks are unknown: the
 *       connection, not the server, decides what arrives.
 *   <li>A frozen server is a choice, not a problem. A late time packet or an estimate below the
 *       share of the target tick rate set in {@link HudSettings#tpsAlarmPercent} is a problem.
 *   <li>Packets waiting, or client ticks taking, as long as the settings' thresholds or longer mean
 *       the client cannot keep up.
 * </ul>
 */
public record NetView(
    int fps,
    Rtt rtt,
    double rttMillis,
    double pingMillis,
    double jitterMillis,
    double stallMillis,
    double timeoutInMillis,
    Tick tick,
    double ticksPerSecond,
    float targetTickRate,
    double tickLateMillis,
    double queueMillis,
    double frameMaxMillis,
    double clientTickMillis,
    boolean bytesKnown,
    double inboundBytesPerSecond,
    double outboundBytesPerSecond,
    double inboundPacketsPerSecond,
    double outboundPacketsPerSecond,
    boolean connectionAlarm,
    boolean tickAlarm,
    boolean queueAlarm,
    boolean clientTickAlarm) {

  /** Whether the client's own line reports a problem. */
  public boolean clientAlarm() {
    return queueAlarm || clientTickAlarm;
  }

  /** The round-trip part. */
  public enum Rtt {
    /** A fresh round trip. */
    VALUE,
    /** No fresh round trip yet. */
    UNKNOWN,
    /** Nothing arrives from the server: see {@link #stallMillis}. */
    STALL,
    /** Single player: there is no round trip. */
    NONE
  }

  /** The server-tick part. */
  public enum Tick {
    /** An estimate from the time packets. */
    ESTIMATE,
    /** Not enough time packets, or the connection is stalled. */
    UNKNOWN,
    /** The server froze its ticking. */
    FROZEN,
    /** The next time packet is late: see {@link #tickLateMillis}. */
    LATE,
    /** Single player is paused. */
    PAUSED
  }

  public static NetView of(
      NetProbe probe,
      ClientStats client,
      int fps,
      boolean memoryConnection,
      boolean paused,
      HudSettings settings) {
    ServerTicks ticks = probe.ticks();
    boolean localPause = memoryConnection && paused;
    double stall = localPause ? Double.NaN : probe.stallMillis();
    boolean stalled = !Double.isNaN(stall);

    Rtt rtt;
    if (memoryConnection) {
      rtt = Rtt.NONE;
    } else if (stalled) {
      rtt = Rtt.STALL;
    } else {
      rtt = Double.isNaN(probe.rttMillis()) ? Rtt.UNKNOWN : Rtt.VALUE;
    }
    double timeout = !memoryConnection && stalled ? probe.timeoutInMillis() : Double.NaN;

    Tick tick;
    double tps = ticks.ticksPerSecond();
    double late = ticks.lateMillis();
    float target = ticks.targetTickRate();
    if (localPause) {
      tick = Tick.PAUSED;
    } else if (stalled && !memoryConnection) {
      tick = Tick.UNKNOWN;
    } else if (ticks.frozen()) {
      tick = Tick.FROZEN;
    } else if (!Double.isNaN(late)) {
      tick = Tick.LATE;
    } else if (Double.isNaN(tps)) {
      tick = Tick.UNKNOWN;
    } else {
      tick = Tick.ESTIMATE;
    }
    boolean tickAlarm =
        tick == Tick.LATE
            || (tick == Tick.ESTIMATE && tps < target * settings.tpsAlarmPercent / 100.0);
    double queueMillis = client.queue().worstMillis();
    boolean queueAlarm = queueMillis >= settings.queueAlarmMs;
    double clientTickMillis = client.ticks().meanMillis();
    boolean clientTickAlarm = clientTickMillis >= settings.clientTickAlarmMs;

    return new NetView(
        fps,
        rtt,
        rtt == Rtt.VALUE ? probe.rttMillis() : Double.NaN,
        rtt == Rtt.VALUE ? probe.pingMillis() : Double.NaN,
        rtt == Rtt.VALUE ? probe.jitterMillis() : Double.NaN,
        stall,
        timeout,
        tick,
        tick == Tick.ESTIMATE ? tps : Double.NaN,
        target,
        tick == Tick.LATE ? late : Double.NaN,
        queueMillis,
        client.frames().maxMillis(),
        clientTickMillis,
        !memoryConnection,
        probe.inboundBytesPerSecond(),
        probe.outboundBytesPerSecond(),
        probe.inboundPacketsPerSecond(),
        probe.outboundPacketsPerSecond(),
        stalled,
        tickAlarm,
        queueAlarm,
        clientTickAlarm);
  }
}
