package ru.wilyfox.lacmus.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import ru.wilyfox.lacmus.Lacmus;
import ru.wilyfox.lacmus.config.LacmusConfig;
import ru.wilyfox.lacmus.core.HudSettings;

/** Every setting of the menu, by page, with its value, change and translation keys. */
final class SettingsCatalog {
  static final String PREFIX = Lacmus.MOD_ID + ".";

  enum Page {
    GENERAL,
    METRICS,
    ALARMS,
    NETWORK;

    Component title() {
      return Component.translatable(PREFIX + "page." + name().toLowerCase(Locale.ROOT));
    }
  }

  enum Kind {
    TOGGLE,
    NUMBER,
    CHOICE
  }

  record Option(
      String id,
      Page page,
      Kind kind,
      Supplier<Component> value,
      IntConsumer change,
      BooleanSupplier selected,
      Supplier<Component> unavailable,
      IntSupplier number,
      IntConsumer setNumber,
      int min,
      int max,
      int step) {
    Component title() {
      return Component.translatable(PREFIX + "setting." + id);
    }

    Component description() {
      return Component.translatable(PREFIX + "setting." + id + ".description");
    }
  }

  private SettingsCatalog() {}

  static List<Option> create() {
    var options = new ArrayList<Option>();
    options.add(toggle("visible", Page.GENERAL, () -> c().visible, v -> c().visible = v));
    options.add(
        choice("mode", Page.GENERAL, HudSettings.Mode.class, () -> c().mode, v -> c().mode = v));
    options.add(
        choice(
            "latency",
            Page.GENERAL,
            HudSettings.Latency.class,
            () -> c().latency,
            v -> c().latency = v));
    options.add(
        choice(
            "anchor",
            Page.GENERAL,
            HudSettings.Anchor.class,
            () -> c().anchor,
            v -> c().anchor = v));
    options.add(
        number(
            "scale",
            Page.GENERAL,
            "percent",
            () -> c().scale,
            v -> c().scale = v,
            HudSettings.MIN_SCALE,
            HudSettings.MAX_SCALE,
            10,
            () -> null));
    options.add(
        number(
            "panel_opacity",
            Page.GENERAL,
            "percent",
            () -> c().panelOpacity,
            v -> c().panelOpacity = v,
            0,
            100,
            5,
            () -> null));

    options.add(metric("show_frame_max", () -> c().showFrameMax, v -> c().showFrameMax = v));
    options.add(metric("show_client_tick", () -> c().showClientTick, v -> c().showClientTick = v));
    options.add(metric("show_queue", () -> c().showQueue, v -> c().showQueue = v));
    options.add(metric("show_jitter", () -> c().showJitter, v -> c().showJitter = v));
    options.add(metric("show_bytes", () -> c().showBytes, v -> c().showBytes = v));
    options.add(metric("show_packets", () -> c().showPackets, v -> c().showPackets = v));
    options.add(metric("show_server", () -> c().showServer, v -> c().showServer = v));

    options.add(toggle("alarms", Page.ALARMS, () -> c().alarms, v -> c().alarms = v));
    options.add(
        number(
            "queue_alarm",
            Page.ALARMS,
            "ms",
            () -> c().queueAlarmMs,
            v -> c().queueAlarmMs = v,
            HudSettings.MIN_QUEUE_ALARM_MS,
            HudSettings.MAX_QUEUE_ALARM_MS,
            50,
            SettingsCatalog::alarmsOff));
    options.add(
        number(
            "client_tick_alarm",
            Page.ALARMS,
            "ms",
            () -> c().clientTickAlarmMs,
            v -> c().clientTickAlarmMs = v,
            HudSettings.MIN_CLIENT_TICK_ALARM_MS,
            HudSettings.MAX_CLIENT_TICK_ALARM_MS,
            5,
            SettingsCatalog::alarmsOff));
    options.add(
        number(
            "tps_alarm",
            Page.ALARMS,
            "percent",
            () -> c().tpsAlarmPercent,
            v -> c().tpsAlarmPercent = v,
            HudSettings.MIN_TPS_ALARM_PERCENT,
            HudSettings.MAX_TPS_ALARM_PERCENT,
            5,
            SettingsCatalog::alarmsOff));

    options.add(
        toggle("ping_requests", Page.NETWORK, () -> c().pingRequests, v -> c().pingRequests = v));
    options.add(
        number(
            "pings_per_second",
            Page.NETWORK,
            "per_second",
            () -> c().pingsPerSecond,
            v -> c().pingsPerSecond = v,
            HudSettings.MIN_PINGS_PER_SECOND,
            HudSettings.MAX_PINGS_PER_SECOND,
            1,
            () -> c().pingRequests ? null : reason("ping_requests_off")));
    return List.copyOf(options);
  }

  static Component onOff(boolean value) {
    return Component.translatable(PREFIX + (value ? "on" : "off"));
  }

  private static Component reason(String id) {
    return Component.translatable(PREFIX + "unavailable." + id);
  }

  private static Component alarmsOff() {
    return c().alarms ? null : reason("alarms_off");
  }

  private static HudSettings c() {
    return LacmusConfig.get();
  }

  /** A part of the full mode: unavailable in the easy mode, which has a fixed line. */
  private static Option metric(String id, BooleanSupplier get, Consumer<Boolean> set) {
    return toggle(
        id,
        Page.METRICS,
        get,
        set,
        () -> c().mode == HudSettings.Mode.EASY ? reason("easy_mode") : null);
  }

  private static Option toggle(String id, Page page, BooleanSupplier get, Consumer<Boolean> set) {
    return toggle(id, page, get, set, () -> null);
  }

  private static Option toggle(
      String id,
      Page page,
      BooleanSupplier get,
      Consumer<Boolean> set,
      Supplier<Component> unavailable) {
    return new Option(
        id,
        page,
        Kind.TOGGLE,
        () -> onOff(get.getAsBoolean()),
        d -> set.accept(!get.getAsBoolean()),
        get,
        unavailable,
        null,
        null,
        0,
        0,
        0);
  }

  private static Option number(
      String id,
      Page page,
      String unit,
      IntSupplier get,
      IntConsumer set,
      int min,
      int max,
      int step,
      Supplier<Component> unavailable) {
    return new Option(
        id,
        page,
        Kind.NUMBER,
        () -> Component.translatable(PREFIX + "unit." + unit, get.getAsInt()),
        direction -> set.accept(Math.clamp(get.getAsInt() + direction * step, min, max)),
        () -> false,
        unavailable,
        get,
        set,
        min,
        max,
        step);
  }

  private static <E extends Enum<E>> Option choice(
      String id, Page page, Class<E> type, Supplier<E> get, Consumer<E> set) {
    E[] values = type.getEnumConstants();
    return new Option(
        id,
        page,
        Kind.CHOICE,
        () ->
            Component.translatable(
                PREFIX + "choice." + id + "." + get.get().name().toLowerCase(Locale.ROOT)),
        direction ->
            set.accept(values[Math.floorMod(get.get().ordinal() + direction, values.length)]),
        () -> false,
        () -> null,
        null,
        null,
        0,
        0,
        0);
  }
}
