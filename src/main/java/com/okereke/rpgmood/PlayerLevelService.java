package com.okereke.rpgmood;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * A separate player-facing progression track from RPGMood's mob-difficulty levels — XP earned
 * from scaled-mob kills and zone discoveries, persisted to {@code playerlevel.yml} (same
 * debounced-YAML pattern as {@link PlayerStatsService}). Deliberately simple: one flat curve,
 * one passive perk (+1 animal cap every 5 levels) rather than a full skill/perk system.
 */
public class PlayerLevelService {

    private static final long SAVE_DELAY_TICKS = 100L; // ~5 seconds debounce
    private static final int ANIMAL_CAP_PER_LEVELS = 5;

    private final RPGMoodPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private boolean saveScheduled = false;

    public PlayerLevelService(RPGMoodPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "playerlevel.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public int getLevel(UUID uuid) {
        return data.getInt("players." + uuid + ".level", 1);
    }

    public int getLevel(Player player) {
        return getLevel(player.getUniqueId());
    }

    public long getXp(Player player) {
        return data.getLong("players." + player.getUniqueId() + ".xp", 0);
    }

    /** Total XP required to reach this level from zero — a mild curve so early levels come quickly and later ones take longer. */
    public static long xpForLevel(int level) {
        return Math.round(100 * Math.pow(level, 1.5));
    }

    /** Passive perk: +1 to the animal ownership cap per 5 levels. Works for offline players too (breeding can happen while the owner is away). */
    public int getAnimalCapBonus(UUID uuid) {
        return getLevel(uuid) / ANIMAL_CAP_PER_LEVELS;
    }

    public int getAnimalCapBonus(Player player) {
        return getAnimalCapBonus(player.getUniqueId());
    }

    /** Adds XP and applies any level-ups (possibly several at once), notifying the player if any occurred. */
    public void addXp(Player player, long amount) {
        if (amount <= 0) return;

        String path = "players." + player.getUniqueId() + ".";
        long xp = data.getLong(path + "xp", 0) + amount;
        int level = data.getInt(path + "level", 1);

        int levelsGained = 0;
        while (xp >= xpForLevel(level + 1)) {
            level++;
            levelsGained++;
        }

        data.set(path + "xp", xp);
        data.set(path + "level", level);
        scheduleSave();

        if (levelsGained > 0) {
            onLevelUp(player, level, levelsGained);
        }
    }

    private void onLevelUp(Player player, int newLevel, int levelsGained) {
        plugin.getMessageService().send(player, "&6&l✦ Level up! You are now level " + newLevel + ".");
        player.playSound(player.getLocation(), "entity.player.levelup", 1.0f, 1.0f);

        int oldLevel = newLevel - levelsGained;
        if (newLevel / ANIMAL_CAP_PER_LEVELS > oldLevel / ANIMAL_CAP_PER_LEVELS) {
            plugin.getMessageService().send(player, Component.text()
                    .append(Component.text("🐾 Your animal cap grew to ", NamedTextColor.GREEN))
                    .append(Component.text(plugin.getConfig().getInt("farming.animals.max_per_player", 20) + getAnimalCapBonus(player), NamedTextColor.WHITE))
                    .append(Component.text("!", NamedTextColor.GREEN))
                    .build());
        }
    }

    private void scheduleSave() {
        if (saveScheduled) return;
        saveScheduled = true;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            saveScheduled = false;
            save();
        }, SAVE_DELAY_TICKS);
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save playerlevel.yml: " + e.getMessage());
        }
    }
}
