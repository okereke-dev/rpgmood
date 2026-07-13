package com.okereke.rpgmood;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Optional sidebar scoreboard (current zone, local danger, session kills) and a temporary
 * BossBar shown on zone change. Both are purely visual — no persisted state, session kills
 * reset on rejoin.
 */
public class ZoneScoreboardService {

    private static final long BOSSBAR_DURATION_TICKS = 80L; // 4 seconds

    private final RPGMoodPlugin plugin;
    private final Map<UUID, Integer> sessionKills = new HashMap<>();

    public ZoneScoreboardService(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled(Player player) {
        return plugin.getConfigManager().getConfigValues().getBoolean("player_scoreboard." + player.getUniqueId(), true);
    }

    /** Rebuilds the sidebar scoreboard for this player, or clears it if the toggle is off. */
    public void updateScoreboard(Player player) {
        if (!isEnabled(player)) {
            clearScoreboard(player);
            return;
        }

        String zone = plugin.getZoneManager().getCurrentZoneDisplayName(player);
        int baseLevel = plugin.getMobScalingService().getBaseLevel(EntityType.ZOMBIE);
        int danger = plugin.getMobScalingService().calculateLevelAt(player.getLocation(), baseLevel);
        int kills = sessionKills.getOrDefault(player.getUniqueId(), 0);

        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = board.registerNewObjective("rpgmood", Criteria.DUMMY, Component.text("RPGMood", NamedTextColor.GOLD));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        objective.getScore("Zone: " + zone).setScore(3);
        objective.getScore("Danger: Lv " + danger).setScore(2);
        objective.getScore("Kills: " + kills).setScore(1);
        player.setScoreboard(board);
    }

    /** Removes RPGMood's sidebar scoreboard, restoring the player's blank/main scoreboard. */
    public void clearScoreboard(Player player) {
        if (player.getScoreboard().getObjective("rpgmood") != null) {
            player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    /** Counts a scaled-mob kill toward this session's tally and refreshes the scoreboard. */
    public void incrementSessionKills(Player player) {
        sessionKills.merge(player.getUniqueId(), 1, Integer::sum);
        updateScoreboard(player);
    }

    /** Shows a temporary BossBar with the zone name, colored the same as its title, for a few seconds. */
    public void showZoneBossBar(Player player, String zoneName, int dangerLevel) {
        if (!isEnabled(player)) return;

        BossBar.Color color = bossBarColorFor(dangerLevel);
        BossBar bossBar = BossBar.bossBar(Component.text(zoneName, NamedTextColor.WHITE), 1.0f, color, BossBar.Overlay.PROGRESS);
        player.showBossBar(bossBar);
        Bukkit.getScheduler().runTaskLater(plugin, () -> player.hideBossBar(bossBar), BOSSBAR_DURATION_TICKS);
    }

    private BossBar.Color bossBarColorFor(int level) {
        if (level <= 3) return BossBar.Color.WHITE;    // Common
        if (level <= 9) return BossBar.Color.YELLOW;   // Uncommon
        if (level <= 17) return BossBar.Color.PURPLE;  // Rare
        if (level <= 27) return BossBar.Color.GREEN;   // Hero
        return BossBar.Color.PINK;                     // Legendary
    }

    /** Clears in-memory session state when a player disconnects. */
    public void remove(UUID uuid) {
        sessionKills.remove(uuid);
    }
}
