package ru.wilyfox.lacmus;

import com.mojang.blaze3d.platform.InputConstants;
import java.nio.file.Path;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import ru.wilyfox.lacmus.config.LacmusConfig;
import ru.wilyfox.lacmus.core.ClientStats;
import ru.wilyfox.lacmus.core.HudSettings;
import ru.wilyfox.lacmus.core.NetProbe;
import ru.wilyfox.lacmus.core.NetView;
import ru.wilyfox.lacmus.core.QueueDelay;
import ru.wilyfox.lacmus.gui.LacmusScreen;
import ru.wilyfox.lacmus.hud.NetHud;
import ru.wilyfox.lacmus.net.NetTaps;
import ru.wilyfox.lacmus.net.Pinger;

/**
 * Loader-neutral client logic. Loader adapters call {@link #init}, register {@link #OPEN_KEY},
 * {@link #TOGGLE_KEY} and the HUD element {@link #HUD_ID}, and forward joins, disconnects, client
 * ticks and HUD drawing here. Everything runs on the client thread, except what {@link NetTaps}
 * puts on the network thread.
 */
public final class LacmusClient {
  public static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(Lacmus.MOD_ID, "net");

  public static final KeyMapping.Category KEY_CATEGORY =
      KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Lacmus.MOD_ID, "main"));

  /** Opens the settings; F6 by default. */
  public static final KeyMapping OPEN_KEY =
      new KeyMapping(
          "key." + Lacmus.MOD_ID + ".open_settings",
          InputConstants.Type.KEYSYM,
          GLFW.GLFW_KEY_F6,
          KEY_CATEGORY);

  /** Shows or hides the HUD; unbound by default. */
  public static final KeyMapping TOGGLE_KEY =
      new KeyMapping(
          "key." + Lacmus.MOD_ID + ".toggle",
          InputConstants.Type.KEYSYM,
          InputConstants.UNKNOWN.getValue(),
          KEY_CATEGORY);

  private static final NetHud HUD = new NetHud();
  private static final ClientStats CLIENT = new ClientStats();
  private static final Pinger PINGER = new Pinger();

  private static NetProbe probe;
  private static String version = "";

  private LacmusClient() {}

  /**
   * Loads the settings from the loader's {@code configDirectory}. {@code modVersion} is shown in
   * the settings screen.
   */
  public static void init(String modVersion, Path configDirectory) {
    version = modVersion;
    LacmusConfig.load(configDirectory);
  }

  public static String version() {
    return version;
  }

  public static HudSettings settings() {
    return LacmusConfig.get();
  }

  public static void onJoin(ClientPacketListener listener) {
    probe = new NetProbe();
    CLIENT.reset();
    NetTaps.install(listener.getConnection(), probe);
    HUD.reset();
    PINGER.reset();
  }

  public static void onDisconnect() {
    probe = null;
    HUD.reset();
  }

  public static void onEndTick(Minecraft minecraft) {
    boolean open = false;
    while (OPEN_KEY.consumeClick()) {
      open = true;
    }
    boolean toggle = false;
    while (TOGGLE_KEY.consumeClick()) {
      toggle = !toggle;
    }
    // Presses while another screen is open are discarded, never queued.
    if (minecraft.player != null && minecraft.gui.screen() == null) {
      if (toggle) {
        toggle(minecraft);
      }
      if (open) {
        minecraft.gui.setScreen(new LacmusScreen(null));
      }
    }

    ClientPacketListener listener = minecraft.getConnection();
    NetProbe current = probe;
    if (current == null || listener == null) {
      return;
    }
    HudSettings settings = settings();
    long now = System.nanoTime();
    if (settings.visible && settings.pingRequests) {
      // Extra ping requests only while the round trip is on screen.
      PINGER.tick(minecraft, listener, now, settings.pingIntervalNanos());
    }
    current.update(now);
    CLIENT.update();
    if (settings.visible) {
      boolean memory = listener.getConnection().isMemoryConnection();
      NetView view =
          NetView.of(current, CLIENT, minecraft.getFps(), memory, minecraft.isPaused(), settings);
      HUD.update(minecraft.font, view, settings);
    }
  }

  private static void toggle(Minecraft minecraft) {
    HudSettings settings = settings();
    settings.visible = !settings.visible;
    try {
      LacmusConfig.save();
    } catch (RuntimeException failure) {
      Lacmus.LOGGER.error("Could not save settings", failure);
    }
    minecraft.gui.hud.setOverlayMessage(
        Component.translatable(Lacmus.MOD_ID + (settings.visible ? ".toggled.on" : ".toggled.off")),
        false);
  }

  /** The probe of the current connection, or {@code null} outside a world. Client thread. */
  public static NetProbe probe() {
    return probe;
  }

  /** The client's own frame, tick and queue statistics. Client thread, except the queue. */
  public static ClientStats clientStats() {
    return CLIENT;
  }

  /** The wait of packets in the client's queue. */
  public static QueueDelay queueDelay() {
    return CLIENT.queue();
  }

  /** Whether {@code processor} is the client's own packet queue. Any thread. */
  public static boolean isClientQueue(Object processor) {
    Minecraft minecraft = Minecraft.getInstance();
    return minecraft != null && processor == minecraft.packetProcessor();
  }

  /** Whether the HUD is switched on. Client thread. */
  public static boolean visible() {
    return settings().visible;
  }

  public static void extractHud(GuiGraphicsExtractor graphics) {
    Minecraft minecraft = Minecraft.getInstance();
    if (!settings().visible || probe == null || minecraft.getDebugOverlay().showDebugScreen()) {
      return;
    }
    HUD.extract(graphics, minecraft.font, settings());
  }
}
