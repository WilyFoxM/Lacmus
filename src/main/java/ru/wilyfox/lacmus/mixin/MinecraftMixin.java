package ru.wilyfox.lacmus.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ru.wilyfox.lacmus.LacmusClient;

/**
 * The client's frame and tick times. The frame time is the game's own, the one it hands to the F3
 * frame chart: the time between the starts of two frames. The game does not time its client tick,
 * so the tick is timed around {@code Minecraft.tick()}. Both run on the client thread.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
  @WrapOperation(
      method = "renderFrame",
      at =
          @At(
              value = "INVOKE",
              target =
                  "Lnet/minecraft/client/gui/components/DebugScreenOverlay;logFrameDuration(J)V"))
  private void lacmus$recordFrame(
      DebugScreenOverlay overlay, long nanos, Operation<Void> original) {
    original.call(overlay, nanos);
    LacmusClient.clientStats().frames().add(nanos);
  }

  @WrapMethod(method = "tick")
  private void lacmus$timeTick(Operation<Void> original) {
    long start = System.nanoTime();
    original.call();
    LacmusClient.clientStats().ticks().add(System.nanoTime() - start);
  }
}
