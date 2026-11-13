package ru.wilyfox.lacmus.hud;

/** Colour roles of the HUD. Palette: #272727, #f3fcf0, #d1495b. */
public final class LacmusTheme {
  /** Panel behind the numbers: #272727 at 75 %. */
  public static final int PANEL = 0xC0272727;

  /** Values. */
  public static final int VALUE = 0xF3FCF0;

  /** Metric names. */
  public static final int LABEL = 0xD1495B;

  /** Base colour handed to text drawing; parts override it. */
  public static final int TEXT_ARGB = 0xFF000000 | VALUE;

  private LacmusTheme() {}
}
