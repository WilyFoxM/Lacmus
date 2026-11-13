package ru.wilyfox.lacmus.net;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.ping.ClientboundPongResponsePacket;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import ru.wilyfox.lacmus.core.NetProbe;

/**
 * Sits right before the game's packet handler: counts decoded packets both ways (a bundle counts as
 * one) and stamps ping requests and pongs on the network thread. Stamping here, rather than in the
 * game's handlers, keeps the wait for the client thread out of the round trip.
 */
final class PacketTap extends ChannelDuplexHandler {
  private final NetProbe probe;

  PacketTap(NetProbe probe) {
    this.probe = probe;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof Packet<?>) {
      long now = System.nanoTime();
      probe.inboundPacket(now);
      if (msg instanceof ClientboundPongResponsePacket pong) {
        probe.pongReceived(pong.time(), now);
      }
    }
    super.channelRead(ctx, msg);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    if (msg instanceof Packet<?>) {
      probe.outboundPacket();
      if (msg instanceof ServerboundPingRequestPacket ping) {
        probe.pingSent(ping.getTime(), System.nanoTime());
      }
    }
    super.write(ctx, msg, promise);
  }
}
