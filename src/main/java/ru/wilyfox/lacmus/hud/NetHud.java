package ru.wilyfox.lacmus.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import ru.wilyfox.lacmus.core.NetFormat;
import ru.wilyfox.lacmus.core.NetProbe;

/**
 * Two lines in the top-left corner, in the spirit of {@code net_graph 1}:
 *
 * <pre>
 * fps 144  rtt 42 ms ±3 ms
 * in 12 kB/s 40/s  out 1.2 kB/s 20/s
 * </pre>
 *
 * The lines are rebuilt once per client tick; a frame only draws them.
 */
public final class NetHud {
  private static final String KEY = "hud.lacmus.";
  private static final int MARGIN = 2;
  private static final int PADDING = 2;
  private static final int LINE_GAP = 1;
  private static final String GAP = "  ";

  private Component top = Component.empty();
  private Component bottom = Component.empty();
  private int width;

  /**
   * @param memoryConnection single player: no bytes on a wire and no round trip
   */
  public void update(Font font, int fps, NetProbe probe, boolean memoryConnection) {
    MutableComponent line = label("fps").append(" ").append(value(NetFormat.whole(fps)));
    line.append(GAP).append(label("rtt")).append(" ");
    String rtt = NetFormat.millis(probe.rttMillis());
    line.append(rtt == null ? unknown() : value("unit.ms", rtt));
    String jitter = NetFormat.millis(probe.jitterMillis());
    if (jitter != null) {
      line.append(" ").append(value("jitter", jitter));
    }
    top = line;

    MutableComponent traffic = label("in").append(" ");
    traffic.append(bytes(probe.inboundBytesPerSecond(), memoryConnection)).append(" ");
    traffic.append(packets(probe.inboundPacketsPerSecond()));
    traffic.append(GAP).append(label("out")).append(" ");
    traffic.append(bytes(probe.outboundBytesPerSecond(), memoryConnection)).append(" ");
    traffic.append(packets(probe.outboundPacketsPerSecond()));
    bottom = traffic;

    width = Math.max(font.width(top), font.width(bottom));
  }

  public void reset() {
    top = Component.empty();
    bottom = Component.empty();
    width = 0;
  }

  public void extract(GuiGraphicsExtractor graphics, Font font) {
    if (width == 0) {
      return;
    }
    int x = MARGIN + PADDING;
    int y = MARGIN + PADDING;
    int lineStep = font.lineHeight + LINE_GAP;
    graphics.fill(
        MARGIN,
        MARGIN,
        x + width + PADDING,
        y + lineStep + font.lineHeight + PADDING - 1,
        LacmusTheme.PANEL);
    graphics.text(font, top, x, y, LacmusTheme.TEXT_ARGB, false);
    graphics.text(font, bottom, x, y + lineStep, LacmusTheme.TEXT_ARGB, false);
  }

  private static Component bytes(double perSecond, boolean memoryConnection) {
    NetFormat.Scaled scaled = memoryConnection ? null : NetFormat.bytesPerSecond(perSecond);
    if (scaled == null) {
      return unknown();
    }
    String unit =
        switch (scaled.unit()) {
          case BYTES -> "unit.bytes";
          case KILOBYTES -> "unit.kilobytes";
          case MEGABYTES -> "unit.megabytes";
        };
    return value(unit, scaled.number());
  }

  private static Component packets(double perSecond) {
    String count = NetFormat.perSecond(perSecond);
    return count == null ? unknown() : value("unit.packets", count);
  }

  private static MutableComponent label(String name) {
    return Component.translatable(KEY + name).withColor(LacmusTheme.LABEL);
  }

  private static MutableComponent value(String number) {
    return Component.literal(number).withColor(LacmusTheme.VALUE);
  }

  private static MutableComponent value(String unitKey, String number) {
    return Component.translatable(KEY + unitKey, number).withColor(LacmusTheme.VALUE);
  }

  private static MutableComponent unknown() {
    return Component.translatable(KEY + "unknown").withColor(LacmusTheme.VALUE);
  }
}
