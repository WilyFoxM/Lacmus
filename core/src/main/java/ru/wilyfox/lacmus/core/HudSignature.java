package ru.wilyfox.lacmus.core;

/**
 * Everything the HUD shows, as numbers: the settings that shape the lines, the states and alarms,
 * and every value as the {@link NetFormat} key of how it is shown. Two views with equal signatures
 * make the same lines, so the HUD rebuilds its lines only when the signature changes. Filling a
 * signature does not allocate.
 *
 * <p>The signature covers every value the HUD can show, whether or not the settings show it; a
 * hidden value that changes costs a rebuild, never a stale line.
 */
public final class HudSignature {
  /** Length of the array {@link #fill} writes. */
  public static final int LENGTH = 31;

  private HudSignature() {}

  /** Writes the signature of {@code view} shown with {@code settings} into {@code out}. */
  public static void fill(NetView view, HudSettings settings, long[] out) {
    int i = 0;
    out[i++] = settings.mode.ordinal();
    out[i++] = settings.latency.ordinal();
    out[i++] =
        bit(settings.showFrameMax, 0)
            | bit(settings.showClientTick, 1)
            | bit(settings.showQueue, 2)
            | bit(settings.showJitter, 3)
            | bit(settings.showBytes, 4)
            | bit(settings.showPackets, 5)
            | bit(settings.showServer, 6)
            | bit(settings.alarms, 7)
            | bit(settings.showServerReport, 8);
    out[i++] =
        bit(view.connectionAlarm(), 0)
            | bit(view.tickAlarm(), 1)
            | bit(view.queueAlarm(), 2)
            | bit(view.clientTickAlarm(), 3)
            | bit(view.bytesKnown(), 4)
            | bit(view.msptAlarm(), 5);
    out[i++] = view.fps();
    out[i++] = view.rtt().ordinal();
    out[i++] = NetFormat.millisKey(view.rttMillis());
    out[i++] = NetFormat.millisKey(view.pingMillis());
    out[i++] = NetFormat.millisKey(view.jitterMillis());
    out[i++] = NetFormat.secondsKey(view.stallMillis());
    out[i++] = NetFormat.secondsKey(view.timeoutInMillis());
    out[i++] = view.tick().ordinal();
    out[i++] = NetFormat.ticksKey(view.ticksPerSecond());
    out[i++] = NetFormat.tickRateKey(view.targetTickRate());
    out[i++] = NetFormat.secondsKey(view.tickLateMillis());
    out[i++] = NetFormat.millisKey(view.queueMillis());
    out[i++] = NetFormat.millisKey(view.frameMaxMillis());
    out[i++] = NetFormat.millisKey(view.clientTickMillis());
    out[i++] = NetFormat.bytesKey(view.inboundBytesPerSecond());
    out[i++] = NetFormat.bytesKey(view.outboundBytesPerSecond());
    out[i++] = NetFormat.perSecondKey(view.inboundPacketsPerSecond());
    out[i++] = NetFormat.perSecondKey(view.outboundPacketsPerSecond());
    out[i++] = settings.queueAlarmMs;
    out[i++] = settings.clientTickAlarmMs;
    out[i++] = settings.tpsAlarmPercent;
    out[i++] = settings.msptAlarmPercent;
    ServerReport report = view.serverReport();
    out[i++] = report == null ? 0 : report.frozen() ? 2 : 1;
    out[i++] = report == null ? 0 : NetFormat.tickRateKey(report.targetTickRate());
    out[i++] = report == null ? 0 : NetFormat.ticksKey(report.ticksPerSecond());
    out[i++] = report == null ? 0 : NetFormat.millisKey(report.msptMean());
    out[i++] = report == null ? 0 : NetFormat.millisKey(report.msptMax());
  }

  private static long bit(boolean flag, int position) {
    return flag ? 1L << position : 0;
  }
}
