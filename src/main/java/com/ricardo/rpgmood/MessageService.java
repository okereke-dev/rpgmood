package com.ricardo.rpgmood;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralises message delivery for all RPGMood ambient messages.
 * Respects the {@code messages.delivery} config option and per-player toggles.
 *
 * Delivery modes:
 *   CHAT       — traditional chat messages (legacy)
 *   ACTION_BAR — modern, non-intrusive overlay above the hotbar (default)
 */
public class MessageService {

    /** How many extra times an action-bar message is resent, and how far apart, so it survives a vanilla fade-out or another plugin's action-bar call landing right after it. */
    private static final int STICKY_RESENDS = 2;
    private static final long STICKY_INTERVAL_TICKS = 20L;

    public enum DeliveryMode {
        CHAT,
        ACTION_BAR
    }

    private final RPGMoodPlugin plugin;
    /** Per-player counter bumped on every action-bar send, so a stale queued resend can detect it's been superseded and skip itself instead of overwriting a newer message. */
    private final Map<UUID, Integer> actionBarGeneration = new ConcurrentHashMap<>();

    public MessageService(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    /** Sends an ambient message to a player using the configured delivery mode. */
    public void send(Player player, String legacyMessage) {
        if (legacyMessage == null || legacyMessage.isBlank() || player == null) return;

        DeliveryMode mode = getEffectiveMode(player);
        Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(legacyMessage);

        switch (mode) {
            case ACTION_BAR -> sendActionBarSticky(player, message);
            case CHAT -> player.sendMessage(message);
        }
    }

    /** Sends an ambient {@link Component} directly. */
    public void send(Player player, Component message) {
        if (message == null || player == null) return;
        DeliveryMode mode = getEffectiveMode(player);

        switch (mode) {
            case ACTION_BAR -> sendActionBarSticky(player, message);
            case CHAT -> player.sendMessage(message);
        }
    }

    /**
     * Sends an action-bar message and resends it a couple more times a second apart, so it
     * doesn't vanish before it's been read if it fades out on its own or another plugin (or
     * another RPGMood system) sends its own action-bar message a moment later.
     */
    private void sendActionBarSticky(Player player, Component message) {
        UUID uuid = player.getUniqueId();
        int generation = actionBarGeneration.merge(uuid, 1, Integer::sum);

        player.sendActionBar(message);
        for (int i = 1; i <= STICKY_RESENDS; i++) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Skip if a newer message has since been sent to this player — otherwise this
                // stale resend would overwrite it instead of reinforcing it.
                if (player.isOnline() && actionBarGeneration.getOrDefault(uuid, -1) == generation) {
                    player.sendActionBar(message);
                }
            }, STICKY_INTERVAL_TICKS * i);
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
