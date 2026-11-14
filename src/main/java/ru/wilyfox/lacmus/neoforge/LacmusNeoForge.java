package ru.wilyfox.lacmus.neoforge;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import ru.wilyfox.lacmus.Lacmus;
import ru.wilyfox.lacmus.LacmusClient;

/** NeoForge adapter: wires loader events to the loader-neutral code. */
@Mod(value = Lacmus.MOD_ID, dist = Dist.CLIENT)
public final class LacmusNeoForge {
  public LacmusNeoForge(IEventBus modBus, ModContainer container) {
    LacmusClient.init(container.getModInfo().getVersion().toString(), FMLPaths.CONFIGDIR.get());

    modBus.addListener(
        RegisterKeyMappingsEvent.class,
        event -> {
          event.registerCategory(LacmusClient.KEY_CATEGORY);
          event.register(LacmusClient.OPEN_KEY);
          event.register(LacmusClient.TOGGLE_KEY);
        });
    modBus.addListener(
        RegisterGuiLayersEvent.class,
        event ->
            event.registerBelow(
                VanillaGuiLayers.CHAT,
                LacmusClient.HUD_ID,
                (graphics, deltaTracker) -> LacmusClient.extractHud(graphics)));

    NeoForge.EVENT_BUS.addListener(
        ClientPlayerNetworkEvent.LoggingIn.class,
        event -> LacmusClient.onJoin(event.getConnection()));
    NeoForge.EVENT_BUS.addListener(
        ClientPlayerNetworkEvent.LoggingOut.class, event -> LacmusClient.onDisconnect());
    NeoForge.EVENT_BUS.addListener(
        ClientTickEvent.Post.class, event -> LacmusClient.onEndTick(Minecraft.getInstance()));
  }
}
