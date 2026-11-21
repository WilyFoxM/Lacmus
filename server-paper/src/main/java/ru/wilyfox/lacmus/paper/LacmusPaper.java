package ru.wilyfox.lacmus.paper;

import com.destroystokyo.paper.event.server.ServerTickEndEvent;
import org.bukkit.ServerTickManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import ru.wilyfox.lacmus.core.ServerReport;
import ru.wilyfox.lacmus.core.ServerTickLog;

/**
 * Lacmus on a Paper server: records how long each server tick takes and, once a second, sends a
 * short report to every player whose client listens on the Lacmus channel. Players without the mod
 * get nothing. Nothing is stored and no command is added.
 */
public final class LacmusPaper extends JavaPlugin implements Listener {
  private static final long REPORT_EVERY_NANOS = 1_000_000_000L;

  private final ServerTickLog log = new ServerTickLog();
  private long lastReport;

  @Override
  public void onEnable() {
    getServer().getMessenger().registerOutgoingPluginChannel(this, ServerReport.CHANNEL);
    getServer().getPluginManager().registerEvents(this, this);
    lastReport = System.nanoTime();
  }

  @Override
  public void onDisable() {
    getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    log.clear();
  }

  /** Main thread, once per tick: no allocation here beyond the event itself. */
  @EventHandler
  public void onTickEnd(ServerTickEndEvent event) {
    long now = System.nanoTime();
    log.tick(now, (long) (event.getTickDuration() * 1_000_000));
    if (now - lastReport < REPORT_EVERY_NANOS) {
      return;
    }
    lastReport = now;
    ServerTickManager ticks = getServer().getServerTickManager();
    byte[] report = log.report(now, ticks.getTickRate(), ticks.isFrozen()).encode();
    for (Player player : getServer().getOnlinePlayers()) {
      // Paper sends only to clients that registered the channel; others are skipped.
      player.sendPluginMessage(this, ServerReport.CHANNEL, report);
    }
  }
}
