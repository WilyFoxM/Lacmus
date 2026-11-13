package ru.wilyfox.lacmus.mixin;

import io.netty.channel.Channel;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The Netty channel behind a connection, to put the measuring handlers into its pipeline. */
@Mixin(Connection.class)
public interface ConnectionAccessor {
  @Accessor("channel")
  Channel lacmus$channel();
}
