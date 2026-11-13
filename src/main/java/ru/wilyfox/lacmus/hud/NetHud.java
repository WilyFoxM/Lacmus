package ru.wilyfox.lacmus.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import ru.wilyfox.lacmus.core.NetFormat;
import ru.wilyfox.lacmus.core.NetView;

/**
 * Lines in the top-left corner, in the spirit of {@code net_graph 1}:
 *
 * <pre>
 * fps 144  rtt 42 ms ±3 ms
 * in 12 kB/s 40/s  out 1.2 kB/s 20/s
 * tps ≈19.9
 * </pre>
 *
 * While nothing arrives from a server, the round trip says for how long, and later how long until
 * the game gives up: {@code rtt stall 8.0 s timeout in 22 s}. In single player, where there is no
 * round trip, the pause is shown on its own: {@code rtt — no data 8.0 s}. A line that reports a
 * problem, a stall or a lagging server, turns entirely to the alarm colour.
 *
 * <p>What to show is decided by {@link NetView}; the lines are rebuilt once per client tick and a
 * frame only draws them.
 */
public final class NetHud {
  private static final String KEY = "hud.lacmus.";
  private static final int MARGIN = 2;
  private static final int PADDING = 2;
  private static final int LINE_GAP = 1;
  private static final String GAP = "  ";

  private Component[] lines = new Component[0];
  private int width;

  public void update(Font font, NetView view) {
    Component connection = connectionLine(view);
    Component ticks = tickLine(view);
    lines =
        new Component[] {
          view.connectionAlarm() ? alarm(connection) : connection,
          trafficLine(view),
          view.tickAlarm() ? alarm(ticks) : ticks
        };
    width = 0;
    for (Component line : lines) {
      width = Math.max(width, font.width(line));
    }
  }

  public void reset() {
    lines = new Component[0];
    width = 0;
  }

  public void extract(GuiGraphicsExtractor graphics, Font font) {
    if (lines.length == 0) {
      return;
    }
    int x = MARGIN + PADDING;
    int y = MARGIN + PADDING;
    int lineStep = font.lineHeight + LINE_GAP;
    int height = lines.length * lineStep - LINE_GAP;
    graphics.fill(MARGIN, MARGIN, x + width + PADDING, y + height + PADDING, LacmusTheme.PANEL);
    for (Component line : lines) {
      graphics.text(font, line, x, y, LacmusTheme.TEXT_ARGB, false);
      y += lineStep;
    }
  }

  private static Component connectionLine(NetView view) {
    MutableComponent line = label("fps").append(" ").append(number(NetFormat.whole(view.fps())));
    line.append(GAP).append(label("rtt")).append(" ");
    String stall = NetFormat.seconds(view.stallMillis());
    switch (view.rtt()) {
      case STALL -> {
        line.append(text("rtt_stall", stall));
        String timeout = NetFormat.seconds(view.timeoutInMillis());
        if (timeout != null) {
          line.append(" ").append(text("timeout", timeout));
        }
      }
      case VALUE -> {
        line.append(text("unit.ms", NetFormat.millis(view.rttMillis())));
        String jitter = NetFormat.millis(view.jitterMillis());
        if (jitter != null) {
          line.append(" ").append(text("jitter", jitter));
        }
      }
      case UNKNOWN, NONE -> {
        line.append(unknown());
        if (stall != null) {
          // Single player has no round trip; the pause is shown on its own.
          line.append(GAP).append(label("stall")).append(" ").append(text("unit.seconds", stall));
        }
      }
    }
    return line;
  }

  private static Component trafficLine(NetView view) {
    MutableComponent line = label("in").append(" ");
    line.append(bytes(view.inboundBytesPerSecond(), view.bytesKnown())).append(" ");
    line.append(packets(view.inboundPacketsPerSecond()));
    line.append(GAP).append(label("out")).append(" ");
    line.append(bytes(view.outboundBytesPerSecond(), view.bytesKnown())).append(" ");
    line.append(packets(view.outboundPacketsPerSecond()));
    return line;
  }

  private static Component tickLine(NetView view) {
    MutableComponent line = label("tps").append(" ");
    return switch (view.tick()) {
      case ESTIMATE -> {
        String estimate = NetFormat.ticks(view.ticksPerSecond());
        float target = view.targetTickRate();
        yield target == 20
            ? line.append(text("tps.estimate", estimate))
            : line.append(text("tps.estimate_of", estimate, NetFormat.tickRate(target)));
      }
      case FROZEN -> line.append(text("tps.frozen"));
      case PAUSED -> line.append(text("tps.paused"));
      case LATE -> line.append(text("tps.late", NetFormat.seconds(view.tickLateMillis())));
      case UNKNOWN -> line.append(unknown());
    };
  }

  private static Component alarm(Component line) {
    return Component.literal(line.getString()).withColor(LacmusTheme.ALARM);
  }

  private static Component bytes(double perSecond, boolean known) {
    NetFormat.Scaled scaled = known ? NetFormat.bytesPerSecond(perSecond) : null;
    if (scaled == null) {
      return unknown();
    }
    String unit =
        switch (scaled.unit()) {
          case BYTES -> "unit.bytes";
          case KILOBYTES -> "unit.kilobytes";
          case MEGABYTES -> "unit.megabytes";
        };
    return text(unit, scaled.number());
  }

  private static Component packets(double perSecond) {
    String count = NetFormat.perSecond(perSecond);
    return count == null ? unknown() : text("unit.packets", count);
  }

  private static MutableComponent label(String name) {
    return Component.translatable(KEY + name).withColor(LacmusTheme.LABEL);
  }

  /** A bare number, the same in every language. */
  private static MutableComponent number(String number) {
    return Component.literal(number).withColor(LacmusTheme.VALUE);
  }

  /** A translated value, with its numbers as arguments. */
  private static MutableComponent text(String key, Object... args) {
    return Component.translatable(KEY + key, args).withColor(LacmusTheme.VALUE);
  }

  private static MutableComponent unknown() {
    return text("unknown");
  }
}
