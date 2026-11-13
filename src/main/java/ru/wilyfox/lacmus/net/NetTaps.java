package ru.wilyfox.lacmus.net;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import net.minecraft.network.Connection;
import net.minecraft.network.HandlerNames;
import ru.wilyfox.lacmus.Lacmus;
import ru.wilyfox.lacmus.core.NetProbe;
import ru.wilyfox.lacmus.mixin.ConnectionAccessor;

/** Puts the measuring handlers into a connection's pipeline. */
public final class NetTaps {
  private static final String WIRE = Lacmus.MOD_ID + ":wire";
  private static final String PACKETS = Lacmus.MOD_ID + ":packets";

  private NetTaps() {}

  /**
   * Starts feeding {@code probe} from {@code connection}. Earlier handlers of this mod, if any, are
   * replaced, so joining again on the same connection does not count twice.
   */
  public static void install(Connection connection, NetProbe probe) {
    Channel channel = ((ConnectionAccessor) connection).lacmus$channel();
    if (channel == null) {
      Lacmus.LOGGER.warn("Connection has no channel; network metrics are off");
      return;
    }
    // Pipeline changes and the handlers themselves run on the channel's event loop.
    channel.eventLoop().execute(() -> install(channel.pipeline(), probe));
  }

  private static void install(ChannelPipeline pipeline, NetProbe probe) {
    if (pipeline.get(WIRE) != null) {
      pipeline.remove(WIRE);
    }
    if (pipeline.get(PACKETS) != null) {
      pipeline.remove(PACKETS);
    }
    pipeline.addFirst(WIRE, new WireTap(probe));
    if (pipeline.get(HandlerNames.PACKET_HANDLER) == null) {
      Lacmus.LOGGER.warn("No packet handler in the pipeline; packet counts and ping are off");
      return;
    }
    pipeline.addBefore(HandlerNames.PACKET_HANDLER, PACKETS, new PacketTap(probe));
  }
}
