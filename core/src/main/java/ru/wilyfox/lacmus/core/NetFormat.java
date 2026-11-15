package ru.wilyfox.lacmus.core;

/**
 * Number formatting for the HUD. Units are returned separately, so the caller can translate them.
 * NaN means "unknown" and formats as {@code null}; callers show a dash.
 *
 * <p>Each number is first rounded to a key, a {@code long} that stands for exactly what is shown;
 * the text is made from the key. Two values show the same text if and only if their keys are equal,
 * so the HUD can tell whether anything it shows has changed without making any text. Keys do not
 * allocate. {@link #UNKNOWN} is the key of an unknown value.
 */
public final class NetFormat {
  /** The key of NaN, shown as a dash. */
  public static final long UNKNOWN = Long.MIN_VALUE;

  /** Adaptive keys below this are tenths, from it on whole numbers times ten. */
  private static final long TENTHS_LIMIT = 100;

  private static final long UNIT_STRIDE = 1L << 40;

  private NetFormat() {}

  /** Decimal units of a byte rate: 1 kB = 1000 B. */
  public enum ByteUnit {
    BYTES,
    KILOBYTES,
    MEGABYTES
  }

  /** A formatted number and its unit. */
  public record Scaled(String number, ByteUnit unit) {}

  /** Bytes per second in B, kB or MB; one decimal below 10 of the unit. */
  public static Scaled bytesPerSecond(double value) {
    long key = bytesKey(value);
    if (key == UNKNOWN) {
      return null;
    }
    ByteUnit unit = ByteUnit.values()[(int) (key / UNIT_STRIDE)];
    long number = key % UNIT_STRIDE;
    return new Scaled(unit == ByteUnit.BYTES ? Long.toString(number) : adaptiveText(number), unit);
  }

  /** The key of {@link #bytesPerSecond}: the unit and the number as shown. */
  public static long bytesKey(double value) {
    if (Double.isNaN(value)) {
      return UNKNOWN;
    }
    double v = Math.max(0, value);
    long bytes = Math.round(v);
    if (bytes < 1000) {
      return bytes;
    }
    if (v < 999_500) {
      return UNIT_STRIDE + adaptiveKey(v / 1000);
    }
    return 2 * UNIT_STRIDE + adaptiveKey(v / 1_000_000);
  }

  /** Milliseconds; one decimal below 10 ms. */
  public static String millis(double value) {
    long key = millisKey(value);
    return key == UNKNOWN ? null : adaptiveText(key);
  }

  /** The key of {@link #millis}. */
  public static long millisKey(double value) {
    return Double.isNaN(value) ? UNKNOWN : adaptiveKey(Math.max(0, value));
  }

  /** Seconds from milliseconds; one decimal below 10 s. */
  public static String seconds(double millis) {
    long key = secondsKey(millis);
    return key == UNKNOWN ? null : adaptiveText(key);
  }

  /** The key of {@link #seconds}. */
  public static long secondsKey(double millis) {
    return Double.isNaN(millis) ? UNKNOWN : adaptiveKey(Math.max(0, millis) / 1000);
  }

  /** Ticks per second with one decimal. */
  public static String ticks(double value) {
    long key = ticksKey(value);
    return key == UNKNOWN ? null : tenthsText(key);
  }

  /** The key of {@link #ticks}: tenths. */
  public static long ticksKey(double value) {
    return Double.isNaN(value) ? UNKNOWN : Math.round(Math.max(0, value) * 10);
  }

  /** A tick rate: whole when it is whole, otherwise one decimal. */
  public static String tickRate(float rate) {
    long key = tickRateKey(rate);
    return (key & 1) == 0 ? Long.toString(key / 2) : tenthsText(key / 2);
  }

  /** The key of {@link #tickRate}: a whole rate times two, or tenths times two plus one. */
  public static long tickRateKey(float rate) {
    return rate == Math.rint(rate) ? Math.round(rate) * 2 : Math.round(rate * 10.0) * 2 + 1;
  }

  /** A whole count per second. */
  public static String perSecond(double value) {
    long key = perSecondKey(value);
    return key == UNKNOWN ? null : Long.toString(key);
  }

  /** The key of {@link #perSecond}. */
  public static long perSecondKey(double value) {
    return Double.isNaN(value) ? UNKNOWN : Math.round(Math.max(0, value));
  }

  /** A whole number. */
  public static String whole(double value) {
    return Long.toString(Math.round(value));
  }

  /** Tenths below 9.95, whole numbers from there; whole numbers are keyed as ten times. */
  private static long adaptiveKey(double value) {
    long tenths = Math.round(value * 10);
    return tenths < TENTHS_LIMIT ? tenths : Math.round(value) * 10;
  }

  private static String adaptiveText(long key) {
    return key < TENTHS_LIMIT ? tenthsText(key) : Long.toString(key / 10);
  }

  private static String tenthsText(long tenths) {
    return (tenths / 10) + "." + (tenths % 10);
  }
}
