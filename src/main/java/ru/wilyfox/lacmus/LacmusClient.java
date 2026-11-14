package ru.wilyfox.lacmus;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.resources.Identifier;
import ru.wilyfox.lacmus.core.NetProbe;
import ru.wilyfox.lacmus.core.NetView;
import ru.wilyfox.lacmus.core.QueueDelay;
import ru.wilyfox.lacmus.hud.NetHud;
import ru.wilyfox.lacmus.net.NetTaps;
import ru.wilyfox.lacmus.net.Pinger;

/**
 * Loader-neutral client logic. Loader adapters register {@link #TOGGLE_KEY} and the HUD element
 * {@link #HUD_ID}, and forward joins, disconnects, client ticks and HUD drawing here. Everything
 * runs on the client thread, except what {@link NetTaps} puts on the network thread.
 */
public final class LacmusClient {
  public static final Identifier HUD_ID = Identifier.fromNamespaceAndPath(Lacmus.MOD_ID, "net");

  public static final KeyMapping.Category KEY_CATEGORY =
      KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Lacmus.MOD_ID, "main"));

  /** Shows or hides the HUD; unbound by default. */
  public static final KeyMapping TOGGLE_KEY =
      new KeyMapping(
          "key." + Lacmus.MOD_ID + ".toggle",
          InputConstants.Type.KEYSYM,
          InputConstants.UNKNOWN.getValue(),
          KEY_CATEGORY);

  private static final NetHud HUD = new NetHud();
  private static final QueueDelay QUEUE = new QueueDelay();
  private static final Pinger PINGER = new Pinger();

  private static NetProbe probe;
  private static boolean visible = true;

  private LacmusClient() {}

  public static void onJoin(ClientPacketListener listener) {
    probe = new NetProbe();
    QUEUE.reset();
    NetTaps.install(listener.getConnection(), probe);
    HUD.reset();
    PINGER.reset();
  }

  public static void onDisconnect() {
    probe = null;
    HUD.reset();
  }

  public static void onEndTick(Minecraft minecraft) {
    while (TOGGLE_KEY.consumeClick()) {
      visible = !visible;
    }
    ClientPacketListener listener = minecraft.getConnection();
    NetProbe current = probe;
    if (current == null || listener == null) {
      return;
    }
    long now = System.nanoTime();
    if (visible) {
      // Extra ping requests only while the round trip is on screen.
      PINGER.tick(minecraft, listener, now);
    }
    current.update(now);
    QUEUE.update();
    if (visible) {
      boolean memory = listener.getConnection().isMemoryConnection();
      NetView view = NetView.of(current, QUEUE, minecraft.getFps(), memory, minecraft.isPaused());
      HUD.update(minecraft.font, view);
    }
  }

  /** The probe of the current connection, or {@code null} outside a world. Client thread. */
  public static NetProbe probe() {
    return probe;
  }

  /** The wait of packets in the client's queue. */
  public static QueueDelay queueDelay() {
    return QUEUE;
  }

  /** Whether {@code processor} is the client's own packet queue. Any thread. */
  public static boolean isClientQueue(Object processor) {
    Minecraft minecraft = Minecraft.getInstance();
    return minecraft != null && processor == minecraft.packetProcessor();
  }

  /** Whether the HUD is switched on. Client thread. */
  public static boolean visible() {
    return visible;
  }

  public static void extractHud(GuiGraphicsExtractor graphics) {
    Minecraft minecraft = Minecraft.getInstance();
    if (!visible || probe == null || minecraft.getDebugOverlay().showDebugScreen()) {
      return;
    }
    HUD.extract(graphics, minecraft.font);
  }
}
