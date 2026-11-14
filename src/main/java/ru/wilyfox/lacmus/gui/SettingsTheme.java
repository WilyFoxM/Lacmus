package ru.wilyfox.lacmus.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Menu colors by role, from the project palette ({@code 272727}, {@code f3fcf0}, {@code d1495b})
 * and darker or lighter shades of it: darker wells and value plates, slightly lighter rows; the
 * accent marks the top edge, the selected page and active values; keyboard focus is a light line.
 */
final class SettingsTheme {
  static final int SCRIM = 0x80141414;
  static final int SHADOW = 0x60000000;
  static final int PANEL = 0xF2272727;
  static final int PANEL_EDGE = 0xFFD1495B;
  static final int SIDEBAR = 0x90141414;
  static final int CONTENT = 0x70141414;
  static final int SURFACE = 0xFF333333;
  static final int HOVER = 0xFF3F3F3F;
  static final int FIELD = 0xFF141414;
  static final int NAV_SELECTED = 0xFF333333;
  static final int NAV_SELECTED_TEXT = 0xFFD1495B;
  static final int VALUE_ON = 0xFF4A2329;
  static final int VALUE_ON_TEXT = 0xFFE8919C;
  static final int VALUE_OFF = 0xFF141414;
  static final int VALUE_OFF_TEXT = 0xFFF3FCF0;
  static final int TITLE = 0xFFD1495B;
  static final int TEXT = 0xFFF3FCF0;
  static final int MUTED = 0xFFADB3AA;
  static final int DISABLED = 0xFF777B75;
  static final int FOCUS = 0xFFF3FCF0;
  static final int SCROLL_TRACK = 0xFF333333;
  static final int SCROLL_THUMB = 0xFFADB3AA;
  static final int DIALOG_SCRIM = 0xD0141414;
  static final int ACCENT = 0xFFD1495B;

  private SettingsTheme() {}

  static void rounded(GuiGraphicsExtractor g, int x, int y, int w, int h, int radius, int color) {
    if (w <= 0 || h <= 0 || color >>> 24 == 0) {
      return;
    }
    int r = Math.clamp(radius, 0, Math.min(w, h) / 2);
    for (int i = 0; i < r; i++) {
      int inset = (int) Math.ceil(r - Math.sqrt(r * r - Math.pow(r - i - .5, 2)));
      g.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
      g.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
    }
    g.fill(x, y + r, x + w, y + h - r, color);
  }

  /** Centered text without the vanilla drop shadow, which smears dark text on light fills. */
  static void centered(
      GuiGraphicsExtractor g, Font font, String text, int centerX, int y, int color) {
    g.text(font, text, centerX - font.width(text) / 2, y, color, false);
  }

  static String fit(Font font, String text, int width) {
    if (width <= 0) {
      return "";
    }
    if (font.width(text) <= width) {
      return text;
    }
    return font.plainSubstrByWidth(text, Math.max(0, width - font.width("…"))) + "…";
  }
}
