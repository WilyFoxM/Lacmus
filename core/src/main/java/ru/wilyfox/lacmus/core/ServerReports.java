package ru.wilyfox.lacmus.core;

/**
 * The client's latest report from the server plugin and when it arrived. Written by whichever
 * thread the loader hands the payload to, read on the client thread: one volatile reference to an
 * immutable pair, so readers never see half a report. A report older than {@link #STALE_NANOS} is
 * not shown: the plugin went away, or the player moved to a server without it.
 */
public final class ServerReports {
  /** A report is shown for this long after it arrived; the plugin sends one a second. */
  public static final long STALE_NANOS = 3_000_000_000L;

  private record Received(ServerReport report, long nanos) {}

  private volatile Received latest;

  /** Takes a payload that arrived at {@code nanos}; ignores one it cannot read. */
  public void accept(byte[] payload, long nanos) {
    ServerReport report = ServerReport.decode(payload);
    if (report != null) {
      latest = new Received(report, nanos);
    }
  }

  /** The report to show at {@code nowNanos}, or null when there is none or it is stale. */
  public ServerReport current(long nowNanos) {
    Received received = latest;
    if (received == null || nowNanos - received.nanos() > STALE_NANOS) {
      return null;
    }
    return received.report();
  }

  /** Forgets the report, as when leaving a server. */
  public void clear() {
    latest = null;
  }
}
