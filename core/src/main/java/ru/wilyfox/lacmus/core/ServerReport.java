package ru.wilyfox.lacmus.core;

import java.nio.ByteBuffer;

/**
 * What the server plugin reports about its own ticks, once a second: the tick rate it aims at,
 * whether ticking is frozen, how many ticks it ran per second over the last five seconds, and how
 * long its recent ticks took. NaN means unknown (the plugin has not run long enough).
 *
 * <p>The wire format is versioned: a client ignores a report whose version it does not know, so a
 * newer plugin never breaks an older client.
 *
 * @param targetTickRate the tick rate the server aims at, 20 unless changed with {@code /tick rate}
 * @param frozen whether the server froze its ticking ({@code /tick freeze})
 * @param ticksPerSecond ticks completed per second over the last five seconds
 * @param msptMean mean duration of the last 20 ticks, in milliseconds
 * @param msptMax longest of the last 20 ticks, in milliseconds
 */
public record ServerReport(
    float targetTickRate, boolean frozen, double ticksPerSecond, double msptMean, double msptMax) {

  /** Version of the wire format written by {@link #encode()}. */
  public static final int VERSION = 1;

  /** The channel the plugin and the client use. */
  public static final String CHANNEL = "lacmus:server";

  private static final int SIZE = 2 + 4 * 4;
  private static final int FROZEN = 1;

  /** The report as bytes: version, flags, then four big-endian floats. */
  public byte[] encode() {
    ByteBuffer out = ByteBuffer.allocate(SIZE);
    out.put((byte) VERSION);
    out.put((byte) (frozen ? FROZEN : 0));
    out.putFloat(targetTickRate);
    out.putFloat((float) ticksPerSecond);
    out.putFloat((float) msptMean);
    out.putFloat((float) msptMax);
    return out.array();
  }

  /** Reads a report, or returns null when the bytes are of another version or malformed. */
  public static ServerReport decode(byte[] bytes) {
    if (bytes == null || bytes.length < SIZE || bytes[0] != VERSION) {
      return null;
    }
    ByteBuffer in = ByteBuffer.wrap(bytes);
    in.get();
    boolean frozen = (in.get() & FROZEN) != 0;
    float target = in.getFloat();
    double tps = in.getFloat();
    double mean = in.getFloat();
    double max = in.getFloat();
    if (!(target > 0) || Float.isInfinite(target)) {
      return null;
    }
    return new ServerReport(target, frozen, tps, mean, max);
  }

  /** The time one tick may take at the target rate, in milliseconds. */
  public double tickBudgetMillis() {
    return 1000.0 / targetTickRate;
  }
}
