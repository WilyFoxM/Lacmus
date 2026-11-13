package ru.wilyfox.lacmus.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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
  void countsAreWhole() {
    assertEquals("40", NetFormat.perSecond(39.7));
  }
}
