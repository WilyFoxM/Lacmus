package ru.wilyfox.lacmus.net;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.ping.ServerboundPingRequestPacket;
import net.minecraft.util.Util;

/**
 * Sends the game's own ping requests while the round trip is shown, because the game sends them
 * only while the F3 network chart is open. Not on an in-memory (single-player) connection, and not
 * while the chart is open: then the game's requests are measured instead.
 */
public final class Pinger {
  private long lastSentNanos;
  private boolean sentAny;

  public void tick(
      Minecraft minecraft, ClientPacketListener listener, long nanos, long intervalNanos) {
    if (listener.getConnection().isMemoryConnection()
        || minecraft.getDebugOverlay().showNetworkCharts()) {
      return;
    }
    if (sentAny && nanos - lastSentNanos < intervalNanos) {
      return;
    }
    sentAny = true;
    lastSentNanos = nanos;
    // The token is a millisecond clock, like the game's own requests, so the game's ping chart
    // stays correct if it also sees these pongs.
    listener.send(new ServerboundPingRequestPacket(Util.getMillis()));
  }

  public void reset() {
    sentAny = false;
  }
}
