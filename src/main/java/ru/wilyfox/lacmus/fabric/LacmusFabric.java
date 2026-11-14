package ru.wilyfox.lacmus.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import ru.wilyfox.lacmus.Lacmus;
import ru.wilyfox.lacmus.LacmusClient;

/** Fabric adapter: wires loader events to the loader-neutral code. */
public final class LacmusFabric implements ClientModInitializer {
  @Override
  public void onInitializeClient() {
    String version =
        FabricLoader.getInstance()
            .getModContainer(Lacmus.MOD_ID)
            .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
            .orElse("");
    LacmusClient.init(version, FabricLoader.getInstance().getConfigDir());
    KeyMapping.Category.register(LacmusClient.KEY_CATEGORY.id());
    KeyMappingHelper.registerKeyMapping(LacmusClient.OPEN_KEY);
    KeyMappingHelper.registerKeyMapping(LacmusClient.TOGGLE_KEY);
    ClientPlayConnectionEvents.JOIN.register(
        (listener, sender, minecraft) -> LacmusClient.onJoin(listener.getConnection()));
    ClientPlayConnectionEvents.DISCONNECT.register(
        (listener, minecraft) -> LacmusClient.onDisconnect());
    ClientTickEvents.END_CLIENT_TICK.register(LacmusClient::onEndTick);
    HudElementRegistry.attachElementBefore(
        VanillaHudElements.CHAT,
        LacmusClient.HUD_ID,
        (graphics, deltaTracker) -> LacmusClient.extractHud(graphics));
  }
}
