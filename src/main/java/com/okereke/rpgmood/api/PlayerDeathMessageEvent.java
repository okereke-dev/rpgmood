package com.okereke.rpgmood.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called after RPGMood generates a narrative death message but before it is set on the event.
 * Other plugins can read or replace the message.
 */
public class PlayerDeathMessageEvent extends PlayerEvent implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private String message;
    private final String causeKey;
    private final String biomeKey;
    private final String killerKey;
    private boolean cancelled;

    public PlayerDeathMessageEvent(@NotNull Player player, String message, String causeKey, String biomeKey, @Nullable String killerKey) {
        super(player);
        this.message = message;
        this.causeKey = causeKey;
        this.biomeKey = biomeKey;
        this.killerKey = killerKey;
    }

    /** The generated death message (may contain colour codes). Replace via {@link #setMessage(String)}. */
    public String getMessage() { return message; }

    /** Override the death message entirely. */
    public void setMessage(String message) { this.message = message; }

    /** Internal cause key (e.g. "entity", "fire", "fall", "void"). */
    public String getCauseKey() { return causeKey; }

    /** Normalised biome key used for message lookup (e.g. "plains", "dark_forest"). */
    public String getBiomeKey() { return biomeKey; }

    /** Killer entity type key, or "none" if environmental death. */
    public String getKillerKey() { return killerKey; }

    @Override
    public boolean isCancelled() { return cancelled; }

    @Override
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
