package com.okereke.rpgmood.farming;

import com.okereke.rpgmood.RPGMoodPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Advances the season by one in-game day every 24000 ticks (20 minutes = 1 MC day).
 * Each season lasts {@code season_length_days} Minecraft days (default 30 ≈ 10 hours real time).
 */
public class SeasonTask extends BukkitRunnable {

    private final RPGMoodPlugin plugin;
    private int mcDayCounter = 0;

    public SeasonTask(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("farming.enabled", true)) {
            return;
        }

        mcDayCounter++;

        // Advance season every 24000 ticks (one full MC day/night cycle)
        if (mcDayCounter >= 24000) {
            mcDayCounter = 0;
            plugin.getSeasonManager().advanceDay();
        }
    }
}
