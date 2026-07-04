package com.ricardo.rpgmood;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * PlaceholderAPI expansion — exposes the player's current zone and the area's mob
 * difficulty for use in scoreboards, tab list, and GUIs. Only registered if
 * PlaceholderAPI is installed.
 *
 * Placeholders:
 *   %rpgmood_zone%          -> current zone display name (e.g. "Elden Meadows")
 *   %rpgmood_area_danger%   -> mob scaling level a zombie would get at the player's location right now
 */
public final class RPGMoodExpansion extends PlaceholderExpansion {

    private final RPGMoodPlugin plugin;

    public RPGMoodExpansion(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "rpgmood";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Ricardo";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        return switch (params) {
            case "zone" -> plugin.getZoneManager().getCurrentZoneDisplayName(player);
            case "area_danger" -> String.valueOf(plugin.getMobScalingService().calculateLevelAt(
                    player.getLocation(), plugin.getMobScalingService().getBaseLevel(EntityType.ZOMBIE)));
            default -> null;
        };
    }
}
