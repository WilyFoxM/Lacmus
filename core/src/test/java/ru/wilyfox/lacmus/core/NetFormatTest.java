package ru.wilyfox.lacmus.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Objects;
import java.util.function.DoubleFunction;
import java.util.function.DoubleToLongFunction;
import org.junit.jupiter.api.Test;
import ru.wilyfox.lacmus.core.NetFormat.ByteUnit;
import ru.wilyfox.lacmus.core.NetFormat.Scaled;

class NetFormatTest {
  @Test
  void unknownIsNull() {
    assertNull(NetFormat.bytesPerSecond(Double.NaN));
    assertNull(NetFormat.millis(Double.NaN));
    assertNull(NetFormat.perSecond(Double.NaN));
  }

  @Test
  void byteRatesPickUnitAndPrecision() {
    assertEquals(new Scaled("0", ByteUnit.BYTES), NetFormat.bytesPerSecond(0));
    assertEquals(new Scaled("999", ByteUnit.BYTES), NetFormat.bytesPerSecond(999.4));
    assertEquals(new Scaled("1.0", ByteUnit.KILOBYTES), NetFormat.bytesPerSecond(999.6));
    assertEquals(new Scaled("9.9", ByteUnit.KILOBYTES), NetFormat.bytesPerSecond(9_940));
    assertEquals(new Scaled("10", ByteUnit.KILOBYTES), NetFormat.bytesPerSecond(9_960));
    assertEquals(new Scaled("999", ByteUnit.KILOBYTES), NetFormat.bytesPerSecond(999_400));
    assertEquals(new Scaled("1.0", ByteUnit.MEGABYTES), NetFormat.bytesPerSecond(999_600));
    assertEquals(new Scaled("12", ByteUnit.MEGABYTES), NetFormat.bytesPerSecond(12_300_000));
  }

  @Test
  void negativeRatesClampToZero() {
    assertEquals(new Scaled("0", ByteUnit.BYTES), NetFormat.bytesPerSecond(-5));
    assertEquals("0", NetFormat.perSecond(-1));
  }

  @Test
  void millisKeepOneDecimalWhenSmall() {
    assertEquals("0.4", NetFormat.millis(0.4));
    assertEquals("9.9", NetFormat.millis(9.94));
    assertEquals("10", NetFormat.millis(9.96));
    assertEquals("142", NetFormat.millis(141.6));
  }

  @Test
  void secondsFromMillis() {
    assertNull(NetFormat.seconds(Double.NaN));
    assertEquals("2.4", NetFormat.seconds(2_400));
    assertEquals("22", NetFormat.seconds(22_000));
    assertEquals("0.0", NetFormat.seconds(0));
  }

  @Test
  void tickRates() {
    assertNull(NetFormat.ticks(Double.NaN));
    assertEquals("19.9", NetFormat.ticks(19.94));
    assertEquals("0.0", NetFormat.ticks(-1));
    assertEquals("10", NetFormat.tickRate(10));
    assertEquals("2.5", NetFormat.tickRate(2.5F));
  }

  @Test
  void countsAreWhole() {
    assertEquals("40", NetFormat.perSecond(39.7));
  }

  @Test
  void keysChangeExactlyWhenTheTextChanges() {
    sweep(NetFormat::millis, NetFormat::millisKey, 0, 2_000, 0.0007);
    sweep(NetFormat::seconds, NetFormat::secondsKey, 0, 60_000, 0.37);
    sweep(NetFormat::ticks, NetFormat::ticksKey, 0, 40, 0.00013);
    sweep(NetFormat::perSecond, NetFormat::perSecondKey, 0, 5_000, 0.013);
    sweep(NetFormat::bytesPerSecond, NetFormat::bytesKey, 0, 30_000_000, 37.3);
    sweep(NetFormat::bytesPerSecond, NetFormat::bytesKey, 900, 1_100, 0.0013);
    sweep(NetFormat::bytesPerSecond, NetFormat::bytesKey, 999_000, 1_001_000, 0.13);
  }

  @Test
  void unknownHasItsOwnKey() {
    assertEquals(NetFormat.UNKNOWN, NetFormat.millisKey(Double.NaN));
    assertEquals(NetFormat.UNKNOWN, NetFormat.bytesKey(Double.NaN));
    assertTrue(NetFormat.millisKey(0) != NetFormat.UNKNOWN);
  }

  @Test
  void tickRateKeysTellWholeFromTenths() {
    assertTrue(NetFormat.tickRateKey(3) != NetFormat.tickRateKey(2.97F));
    assertEquals("3.0", NetFormat.tickRate(2.97F));
    assertEquals("3", NetFormat.tickRate(3));
  }

  /** Walks the range in small steps: the key and the text must change at the same steps. */
  private static void sweep(
      DoubleFunction<Object> text, DoubleToLongFunction key, double from, double to, double step) {
    Object lastText = text.apply(from);
    long lastKey = key.applyAsLong(from);
    int changes = 0;
    for (double v = from + step; v <= to; v += step) {
      Object t = text.apply(v);
      long k = key.applyAsLong(v);
      boolean textChanged = !Objects.equals(t, lastText);
      boolean keyChanged = k != lastKey;
      double at = v;
      Object before = lastText;
      assertEquals(
          textChanged, keyChanged, () -> "at " + at + ": " + before + " -> " + t + ", key " + k);
      changes += textChanged ? 1 : 0;
      lastText = t;
      lastKey = k;
    }
    assertTrue(changes > 0, "the sweep crossed a change of the text");
  }
}
