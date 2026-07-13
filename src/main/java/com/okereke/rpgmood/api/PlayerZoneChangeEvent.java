package com.okereke.rpgmood.api;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Called when a player changes from one RPGMood zone to another (after the dwell timer has elapsed).
 * The new zone's display name and internal key are both exposed.
 */
public class PlayerZoneChangeEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String previousZone;
    private final String newZone;
    private final String previousZoneDisplay;
    private final String newZoneDisplay;
    private final boolean isDynamic;

    public PlayerZoneChangeEvent(@NotNull Player player, String previousZone, String newZone,
                                 String previousZoneDisplay, String newZoneDisplay, boolean isDynamic) {
        super(player);
        this.previousZone = previousZone;
        this.newZone = newZone;
        this.previousZoneDisplay = previousZoneDisplay;
        this.newZoneDisplay = newZoneDisplay;
        this.isDynamic = isDynamic;
    }

    /** The internal zone key the player left (may be null on first join). */
    public String getPreviousZone() { return previousZone; }

    /** The internal zone key the player entered. */
    public String getNewZone() { return newZone; }

    /** The human-readable display name of the previous zone. */
    public String getPreviousZoneDisplay() { return previousZoneDisplay; }

    /** The human-readable display name of the new zone. */
    public String getNewZoneDisplay() { return newZoneDisplay; }

    /** True if the new zone is a procedurally-generated dynamic zone (not from zones.yml). */
    public boolean isDynamic() { return isDynamic; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
