package com.okereke.rpgmood;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Replaces vanilla's chat join/quit messages with custom ones (message + sound), delivered
 * to every online player via {@link MessageService} — respects each viewer's action-bar/chat
 * preference the same way any other ambient message does.
 */
public class PlayerConnectionListener implements Listener {

    private final RPGMoodPlugin plugin;

    public PlayerConnectionListener(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.joinMessage(null);
        Player player = event.getPlayer();

        boolean firstTime = !player.hasPlayedBefore();
        String template = plugin.getConfig().getString(
                firstTime ? "messages.player_join_first_time" : "messages.player_join",
                "&a→ {player} joined the world.");
        String sound = plugin.getConfig().getString(
                firstTime ? "messages.join_first_time_sound" : "messages.join_sound",
                "entity.player.levelup");

        broadcastConnectionEvent(player, template, sound);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.quitMessage(null);
        Player player = event.getPlayer();

        String template = plugin.getConfig().getString("messages.player_quit", "&7← {player} left the world.");
        String sound = plugin.getConfig().getString("messages.quit_sound", "entity.villager.no");

        broadcastConnectionEvent(player, template, sound);
    }

    private void broadcastConnectionEvent(Player player, String template, String sound) {
        String message = template.replace("{player}", player.getName());
        plugin.getMessageService().broadcastAmbient(message);

        if (sound == null || sound.isBlank()) return;
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.playSound(online.getLocation(), sound, 1.0f, 1.0f);
        }
    }
}
