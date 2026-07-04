package com.ricardo.rpgmood;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Persists lightweight server-wide player stats (deaths, zone changes, highest mob level killed) for /rpgmood leaderboard. */
public class PlayerStatsService {

    private final RPGMoodPlugin plugin;
    private final File statsFile;
    private YamlConfiguration stats;

    public PlayerStatsService(RPGMoodPlugin plugin) {
        this.plugin = plugin;
        this.statsFile = new File(plugin.getDataFolder(), "stats.yml");
        stats = YamlConfiguration.loadConfiguration(statsFile);
    }

    public void recordDeath(Player player) {
        String path = playerPath(player);
        stats.set(path + "deaths", stats.getInt(path + "deaths", 0) + 1);
        save();
    }

    public void recordZoneChange(Player player) {
        String path = playerPath(player);
        stats.set(path + "zone_changes", stats.getInt(path + "zone_changes", 0) + 1);
        save();
    }

    public void recordMobKill(Player player, int level) {
        String path = playerPath(player);
        if (level > stats.getInt(path + "highest_mob_level", 0)) {
            stats.set(path + "highest_mob_level", level);
            save();
        }
    }

    public List<StatEntry> getTopDeaths(int limit) {
        return getTop("deaths", limit);
    }

    public List<StatEntry> getTopZoneChanges(int limit) {
        return getTop("zone_changes", limit);
    }

    public List<StatEntry> getTopMobLevel(int limit) {
        return getTop("highest_mob_level", limit);
    }

    private String playerPath(Player player) {
        String path = "players." + player.getUniqueId() + ".";
        stats.set(path + "name", player.getName());
        return path;
    }

    private List<StatEntry> getTop(String field, int limit) {
        List<StatEntry> entries = new ArrayList<>();
        ConfigurationSection playersSection = stats.getConfigurationSection("players");
        if (playersSection != null) {
            for (String uuid : playersSection.getKeys(false)) {
                String name = playersSection.getString(uuid + ".name", uuid);
                int value = playersSection.getInt(uuid + "." + field, 0);
                if (value > 0) {
                    entries.add(new StatEntry(name, value));
                }
            }
        }
        entries.sort(Comparator.comparingInt(StatEntry::value).reversed());
        return entries.size() > limit ? entries.subList(0, limit) : entries;
    }

    private void save() {
        try {
            stats.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save stats.yml: " + e.getMessage());
        }
    }

    public record StatEntry(String name, int value) {}
}
