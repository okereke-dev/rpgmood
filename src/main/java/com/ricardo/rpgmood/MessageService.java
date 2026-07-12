package com.ricardo.rpgmood;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

/**
 * Centralises message delivery for all RPGMood ambient messages.
 * Respects the {@code messages.delivery} config option and per-player toggles.
 *
 * Delivery modes:
 *   CHAT       — traditional chat messages (legacy)
 *   ACTION_BAR — modern, non-intrusive overlay above the hotbar (default)
 */
public class MessageService {

    public enum DeliveryMode {
        CHAT,
        ACTION_BAR
    }

    private final RPGMoodPlugin plugin;

    public MessageService(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    /** Sends an ambient message to a player using the configured delivery mode. */
    public void send(Player player, String legacyMessage) {
        if (legacyMessage == null || legacyMessage.isBlank() || player == null) return;

        DeliveryMode mode = getEffectiveMode(player);
        Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(legacyMessage);

        switch (mode) {
            case ACTION_BAR -> player.sendActionBar(message);
            case CHAT -> player.sendMessage(message);
        }
    }

    /** Sends an ambient {@link Component} directly. */
    public void send(Player player, Component message) {
        if (message == null || player == null) return;
        DeliveryMode mode = getEffectiveMode(player);

        switch (mode) {
            case ACTION_BAR -> player.sendActionBar(message);
            case CHAT -> player.sendMessage(message);
        }
    }

    /** Sends a system/broadcast message that always goes to chat regardless of delivery mode. */
    public void sendChat(Player player, String legacyMessage) {
        if (legacyMessage == null || legacyMessage.isBlank() || player == null) return;
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(legacyMessage));
    }

    /** Broadcasts an important server-wide message to chat (bypasses delivery mode). */
    public void broadcast(String legacyMessage) {
        if (legacyMessage == null || legacyMessage.isBlank()) return;
        org.bukkit.Bukkit.broadcast(LegacyComponentSerializer.legacyAmpersand().deserialize(legacyMessage));
    }

    /** Returns the effective delivery mode for a player, checking config + per-player toggle. */
    private DeliveryMode getEffectiveMode(Player player) {
        // Per-player toggle overrides global config
        String toggleKey = "player_actionbar." + player.getUniqueId();
        if (plugin.getConfigManager().getConfigValues().contains(toggleKey)) {
            return plugin.getConfigManager().getConfigValues().getBoolean(toggleKey, true)
                    ? DeliveryMode.ACTION_BAR
                    : DeliveryMode.CHAT;
        }

        // Global config default
        String modeStr = plugin.getConfig().getString("messages.delivery", "ACTION_BAR");
        try {
            return DeliveryMode.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DeliveryMode.ACTION_BAR;
        }
    }

    /** Toggles a player between ACTION_BAR and CHAT. Returns the new mode. */
    public DeliveryMode toggle(Player player) {
        boolean useActionBar = !plugin.getConfigManager().getConfigValues().getBoolean("player_actionbar." + player.getUniqueId(), true);
        plugin.getConfigManager().savePlayerToggle("player_actionbar.", player.getUniqueId(), useActionBar);
        return useActionBar ? DeliveryMode.ACTION_BAR : DeliveryMode.CHAT;
    }
}
