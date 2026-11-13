package ru.wilyfox.lacmus.core;

import java.util.Locale;

/**
 * Number formatting for the HUD. Units are returned separately, so the caller can translate them.
 * NaN means "unknown" and formats as {@code null}; callers show a dash.
 */
public final class NetFormat {
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
    if (Double.isNaN(value)) {
      return null;
    }
    double v = Math.max(0, value);
    if (Math.round(v) < 1000) {
      return new Scaled(whole(v), ByteUnit.BYTES);
    }
    if (v < 999_500) {
      return new Scaled(adaptive(v / 1000), ByteUnit.KILOBYTES);
    }
    return new Scaled(adaptive(v / 1_000_000), ByteUnit.MEGABYTES);
  }

  /** Milliseconds; one decimal below 10 ms. */
  public static String millis(double value) {
    return Double.isNaN(value) ? null : adaptive(Math.max(0, value));
  }

  /** Seconds from milliseconds; one decimal below 10 s. */
  public static String seconds(double millis) {
    return Double.isNaN(millis) ? null : adaptive(Math.max(0, millis) / 1000);
  }

  /** Ticks per second with one decimal. */
  public static String ticks(double value) {
    return Double.isNaN(value) ? null : String.format(Locale.ROOT, "%.1f", Math.max(0, value));
  }

  /** A tick rate: whole when it is whole, otherwise one decimal. */
  public static String tickRate(float rate) {
    return rate == Math.rint(rate) ? whole(rate) : String.format(Locale.ROOT, "%.1f", rate);
  }

  /** A whole count per second. */
  public static String perSecond(double value) {
    return Double.isNaN(value) ? null : whole(Math.max(0, value));
  }

  /** A whole number. */
  public static String whole(double value) {
    return String.format(Locale.ROOT, "%.0f", value);
  }

  private static String adaptive(double value) {
    return value < 9.95
        ? String.format(Locale.ROOT, "%.1f", value)
        : String.format(Locale.ROOT, "%.0f", value);
  }
}
