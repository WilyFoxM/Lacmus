package ru.wilyfox.lacmus.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import ru.wilyfox.lacmus.core.ServerReport;

/**
 * The server plugin's report as it arrives: the raw bytes of a plugin message on {@link
 * ServerReport#CHANNEL}. Each loader adapter registers {@link #TYPE} with {@link #CODEC} and hands
 * the bytes to {@code LacmusClient.onServerReport}; reading them is the core's job.
 */
public record ServerReportPayload(byte[] data) implements CustomPacketPayload {
  public static final Type<ServerReportPayload> TYPE =
      new Type<>(Identifier.parse(ServerReport.CHANNEL));

  /** The whole payload is the report; a plugin message has no other framing. */
  public static final StreamCodec<FriendlyByteBuf, ServerReportPayload> CODEC =
      StreamCodec.of(
          (buffer, payload) -> buffer.writeBytes(payload.data()),
          buffer -> {
            byte[] data = new byte[buffer.readableBytes()];
            buffer.readBytes(data);
            return new ServerReportPayload(data);
          });

  @Override
  public Type<ServerReportPayload> type() {
    return TYPE;
  }
}
