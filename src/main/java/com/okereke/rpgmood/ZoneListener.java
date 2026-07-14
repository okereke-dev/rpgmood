package com.okereke.rpgmood;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ZoneListener implements Listener {

    private final RPGMoodPlugin plugin;

    public ZoneListener(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        plugin.getZoneManager().handlePlayerZone(event.getPlayer());
        plugin.getSubzoneManager().handlePlayerMove(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getZoneManager().handlePlayerQuit(event.getPlayer());
        plugin.getZoneScoreboardService().remove(event.getPlayer().getUniqueId());
        plugin.getSubzoneManager().handlePlayerQuit(event.getPlayer());
    }
}
