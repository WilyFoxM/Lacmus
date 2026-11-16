package ru.wilyfox.lacmus.hud;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FormattedCharSequence;
import ru.wilyfox.lacmus.core.HudSettings;
import ru.wilyfox.lacmus.core.HudSignature;
import ru.wilyfox.lacmus.core.NetFormat;
import ru.wilyfox.lacmus.core.NetView;

/**
 * The HUD, in the spirit of {@code net_graph}. The full mode has one line per side of the game:
 *
 * <pre>
 * fps 144 (max 18 ms)  tick 2.1 ms  queue 4 ms           the client
 * rtt 42 ms ±3 ms  in 12 kB/s 40/s  out 1.2 kB/s 20/s   the connection
 * tps ≈19.9                                              the server
 * </pre>
 *
 * The easy-to-read mode has one: {@code fps 144 ping 42 ms ±3 ms}. The latency is {@code rtt}, the
 * newest round trip, or {@code ping}, the mean of the last five seconds, as set.
 *
 * <p>While nothing arrives from a server, the latency says for how long, and later how long until
 * the game gives up: {@code rtt stall 8.0 s timeout in 22 s}. In single player, where there is no
 * round trip, the pause is shown on its own: {@code rtt — no data 8.0 s}. A line that reports a
 * problem on its side turns entirely to the alarm colour, unless alarms are off.
 *
 * <p>What to show is decided by {@link NetView} and {@link HudSettings}. Once per client tick the
 * HUD compares the {@link HudSignature} of what it would show with the last one and rebuilds its
 * lines only when they differ, or when the language changed; a frame draws the prepared text and
 * makes none.
 */
public final class NetHud {
  private static final String KEY = "hud.lacmus.";
  private static final int MARGIN = 2;
  private static final int PADDING = 2;
  private static final int LINE_GAP = 1;
  private static final String GAP = "  ";

  private final List<Component> lines = new ArrayList<>(3);
  private final List<FormattedCharSequence> drawn = new ArrayList<>(3);
  private long[] signature = new long[HudSignature.LENGTH];
  private long[] shown = new long[HudSignature.LENGTH];
  private Language language;
  private int width;

  public void update(Font font, NetView view, HudSettings settings) {
    HudSignature.fill(view, settings, signature);
    Language current = Language.getInstance();
    if (!drawn.isEmpty() && current == language && Arrays.equals(signature, shown)) {
      return;
    }
    long[] swap = shown;
    shown = signature;
    signature = swap;
    language = current;
    lines.clear();
    boolean alarms = settings.alarms;
    if (settings.mode == HudSettings.Mode.EASY) {
      MutableComponent line = fps(view);
      line.append(GAP);
      latency(line, view, settings, true);
      lines.add(alarms && view.connectionAlarm() ? alarm(line) : line);
    } else {
      Component client = clientLine(view, settings);
      Component network = networkLine(view, settings);
      lines.add(alarms && view.clientAlarm() ? alarm(client) : client);
      lines.add(alarms && view.connectionAlarm() ? alarm(network) : network);
      if (settings.showServer) {
        Component server = serverLine(view);
        lines.add(alarms && view.tickAlarm() ? alarm(server) : server);
      }
    }
    drawn.clear();
    width = 0;
    for (Component line : lines) {
      FormattedCharSequence text = line.getVisualOrderText();
      drawn.add(text);
      width = Math.max(width, font.width(text));
    }
  }

  public void reset() {
    lines.clear();
    drawn.clear();
    language = null;
    width = 0;
  }

  public void extract(GuiGraphicsExtractor graphics, Font font, HudSettings settings) {
    if (drawn.isEmpty()) {
      return;
    }
    int lineStep = font.lineHeight + LINE_GAP;
    int panelWidth = width + 2 * PADDING;
    int panelHeight = drawn.size() * lineStep - LINE_GAP + 2 * PADDING;
    // A size too large for the window shrinks to fit it, so no line runs off the screen.
    float fit =
        Math.min(
            (graphics.guiWidth() - 2f * MARGIN) / panelWidth,
            (graphics.guiHeight() - 2f * MARGIN) / panelHeight);
    float scale = Math.min(settings.scale / 100f, fit);
    float left =
        switch (settings.anchor) {
          case TOP_LEFT, BOTTOM_LEFT -> MARGIN;
          case TOP_RIGHT, BOTTOM_RIGHT -> graphics.guiWidth() - MARGIN - panelWidth * scale;
        };
    float top =
        switch (settings.anchor) {
          case TOP_LEFT, TOP_RIGHT -> MARGIN;
          case BOTTOM_LEFT, BOTTOM_RIGHT -> graphics.guiHeight() - MARGIN - panelHeight * scale;
        };
    graphics.pose().pushMatrix();
    graphics.pose().translate(left, top);
    graphics.pose().scale(scale, scale);
    int alpha = Math.round(settings.panelOpacity * 255 / 100f);
    if (alpha > 0) {
      graphics.fill(0, 0, panelWidth, panelHeight, alpha << 24 | LacmusTheme.PANEL_RGB);
    }
    int y = PADDING;
    for (int i = 0; i < drawn.size(); i++) {
      graphics.text(font, drawn.get(i), PADDING, y, LacmusTheme.TEXT_ARGB, false);
      y += lineStep;
    }
    graphics.pose().popMatrix();
  }

  private static MutableComponent fps(NetView view) {
    return label("fps").append(" ").append(number(NetFormat.whole(view.fps())));
  }

  /** The client itself: frames, ticks, and how long packets wait for it. */
  private static Component clientLine(NetView view, HudSettings settings) {
    MutableComponent line = fps(view);
    String frameMax = NetFormat.millis(view.frameMaxMillis());
    if (settings.showFrameMax && frameMax != null) {
      line.append(" ").append(text("frame_max", frameMax));
    }
    if (settings.showClientTick) {
      line.append(GAP).append(label("client_tick")).append(" ");
      String tick = NetFormat.millis(view.clientTickMillis());
      line.append(tick == null ? unknown() : text("unit.ms", tick));
    }
    if (settings.showQueue) {
      line.append(GAP).append(label("queue")).append(" ");
      String queue = NetFormat.millis(view.queueMillis());
      line.append(queue == null ? unknown() : text("unit.ms", queue));
    }
    return line;
  }

  /** The connection: latency or stall, and traffic both ways. */
  private static Component networkLine(NetView view, HudSettings settings) {
    MutableComponent line = Component.empty();
    latency(line, view, settings, settings.showJitter);
    if (settings.showBytes || settings.showPackets) {
      line.append(GAP).append(label("in")).append(" ");
      traffic(line, view.inboundBytesPerSecond(), view.inboundPacketsPerSecond(), view, settings);
      line.append(GAP).append(label("out")).append(" ");
      traffic(line, view.outboundBytesPerSecond(), view.outboundPacketsPerSecond(), view, settings);
    }
    return line;
  }

  /** {@code rtt} or {@code ping}, with the jitter, a stall, or a pause in single player. */
  private static void latency(
      MutableComponent line, NetView view, HudSettings settings, boolean jitter) {
    boolean ping = settings.latency == HudSettings.Latency.PING;
    line.append(label(ping ? "ping" : "rtt")).append(" ");
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
        String value = NetFormat.millis(ping ? view.pingMillis() : view.rttMillis());
        line.append(value == null ? unknown() : text("unit.ms", value));
        String spread = NetFormat.millis(view.jitterMillis());
        if (jitter && spread != null) {
          line.append(" ").append(text("jitter", spread));
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
  }

  private static void traffic(
      MutableComponent line,
      double bytesPerSecond,
      double packetsPerSecond,
      NetView view,
      HudSettings settings) {
    if (settings.showBytes) {
      line.append(bytes(bytesPerSecond, view.bytesKnown()));
      if (settings.showPackets) {
        line.append(" ");
      }
    }
    if (settings.showPackets) {
      line.append(packets(packetsPerSecond));
    }
  }

  /** The server: how fast it ticks. */
  private static Component serverLine(NetView view) {
    MutableComponent line = label("tps").append(" ");
    return line.append(
        switch (view.tick()) {
          case ESTIMATE -> {
            String estimate = NetFormat.ticks(view.ticksPerSecond());
            float target = view.targetTickRate();
            yield target == 20
                ? text("tps.estimate", estimate)
                : text("tps.estimate_of", estimate, NetFormat.tickRate(target));
          }
          case FROZEN -> text("tps.frozen");
          case PAUSED -> text("tps.paused");
          case LATE -> text("tps.late", NetFormat.seconds(view.tickLateMillis()));
          case UNKNOWN -> unknown();
        });
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
