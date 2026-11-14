package ru.wilyfox.lacmus.forge;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.AddGuiOverlayLayersEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import ru.wilyfox.lacmus.Lacmus;
import ru.wilyfox.lacmus.LacmusClient;

/**
 * Forge adapter: wires loader events to the loader-neutral code. Lacmus is client-side only; on a
 * dedicated server it does nothing.
 */
@Mod(Lacmus.MOD_ID)
public final class LacmusForge {
  public LacmusForge(FMLJavaModLoadingContext context) {
    if (FMLEnvironment.dist != Dist.CLIENT) {
      return;
    }
    LacmusClient.init(
        context.getContainer().getModInfo().getVersion().toString(), FMLPaths.CONFIGDIR.get());

    RegisterKeyMappingsEvent.BUS.addListener(
        event -> {
          KeyMapping.Category.register(LacmusClient.KEY_CATEGORY.id());
          event.register(LacmusClient.OPEN_KEY);
          event.register(LacmusClient.TOGGLE_KEY);
        });
    AddGuiOverlayLayersEvent.BUS.addListener(
        event ->
            event
                .getLayeredDraw()
                .add(
                    LacmusClient.HUD_ID,
                    (graphics, deltaTracker) -> LacmusClient.extractHud(graphics)));

    ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(
        event -> LacmusClient.onJoin(event.getConnection()));
    ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> LacmusClient.onDisconnect());
    TickEvent.ClientTickEvent.Post.BUS.addListener(
        event -> LacmusClient.onEndTick(Minecraft.getInstance()));
  }
}
