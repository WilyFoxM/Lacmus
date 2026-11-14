package ru.wilyfox.lacmus.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Queue;
import net.minecraft.network.PacketProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import ru.wilyfox.lacmus.LacmusClient;

/**
 * Times how long packets wait in the client's queue: stamped when the network thread queues them,
 * measured when the client thread takes them out to handle. The integrated server of single player
 * uses the same class; only the client's own queue is measured.
 */
@Mixin(PacketProcessor.class)
public abstract class PacketProcessorMixin {
  @WrapOperation(
      method = "scheduleIfPossible",
      at = @At(value = "INVOKE", target = "Ljava/util/Queue;add(Ljava/lang/Object;)Z"))
  private boolean lacmus$stampQueued(
      Queue<Object> queue, Object item, Operation<Boolean> original) {
    boolean added = original.call(queue, item);
    if (added && LacmusClient.isClientQueue(this)) {
      LacmusClient.queueDelay().queued(item, System.nanoTime());
    }
    return added;
  }

  @WrapOperation(
      method = "processQueuedPackets",
      at = @At(value = "INVOKE", target = "Ljava/util/Queue;poll()Ljava/lang/Object;"))
  private Object lacmus$measureHandled(Queue<Object> queue, Operation<Object> original) {
    Object item = original.call(queue);
    if (item != null && LacmusClient.isClientQueue(this)) {
      LacmusClient.queueDelay().handled(item, System.nanoTime());
    }
    return item;
  }
}
