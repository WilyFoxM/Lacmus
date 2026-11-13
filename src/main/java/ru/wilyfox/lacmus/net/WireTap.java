package ru.wilyfox.lacmus.net;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import ru.wilyfox.lacmus.core.NetProbe;

/**
 * Counts bytes at the socket end of the pipeline: what crosses the wire after compression and
 * encryption, without TCP/IP headers. In-memory connections carry no bytes and count nothing.
 */
final class WireTap extends ChannelDuplexHandler {
  private final NetProbe probe;

  WireTap(NetProbe probe) {
    this.probe = probe;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof ByteBuf bytes) {
      probe.inboundBytes(bytes.readableBytes());
    }
    super.channelRead(ctx, msg);
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
      throws Exception {
    if (msg instanceof ByteBuf bytes) {
      probe.outboundBytes(bytes.readableBytes());
    }
    super.write(ctx, msg, promise);
  }
}
