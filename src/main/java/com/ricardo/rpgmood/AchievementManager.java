package com.ricardo.rpgmood;

import com.ricardo.rpgmood.farming.CropQuality;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tracks and persists player achievements related to exploration, combat, farming, and survival.
 * Unlock state and progress counters are stored in achievements.yml, debounced-saved like {@link PlayerStatsService}
 * since combat/farming progress can be written on every kill or harvest.
 */
public class AchievementManager {

    public record Achievement(String id, String name, String description, String icon) {}

    public static final Set<Achievement> ALL_ACHIEVEMENTS = Set.of(
            new Achievement("first_steps", "First Steps", "Enter your first zone", "🚶"),
            new Achievement("world_explorer", "World Explorer", "Discover 10 different zones", "🗺️"),
            new Achievement("seasoned_traveler", "Seasoned Traveler", "Discover 25 different zones", "🌍"),
            new Achievement("slayer_initiate", "Slayer Initiate", "Kill 10 scaled mobs", "⚔️"),
            new Achievement("slayer_veteran", "Slayer Veteran", "Kill 100 scaled mobs", "🗡️"),
            new Achievement("slayer_legend", "Slayer Legend", "Kill 500 scaled mobs", "👑"),
            new Achievement("danger_seeker", "Danger Seeker", "Kill a level 30+ mob", "🔥"),
            new Achievement("void_walker", "Void Walker", "Survive the End", "🌌"),
            new Achievement("immortal", "Immortal", "Survive 24 MC days without dying", "⭐"),
            new Achievement("farmer", "Farmer", "Harvest 50 crops", "🌾"),
            new Achievement("master_farmer", "Master Farmer", "Harvest a Gold-quality crop", "🌟"),
            new Achievement("chef", "Chef", "Cook 10 recipes", "🍳"),
            new Achievement("gourmet", "Gourmet", "Cook all recipes", "👨‍🍳"),
            new Achievement("seasons_first", "First of the Season", "Witness a season change", "🌸"),
            new Achievement("night_owl", "Night Owl", "Survive 30 nights", "🦉")
    );

    private static final long SAVE_DELAY_TICKS = 100L; // ~5 seconds debounce, mirrors PlayerStatsService

    private final RPGMoodPlugin plugin;
    private final File dataFile;
    private YamlConfiguration data;
    private boolean saveScheduled = false;

    public AchievementManager(RPGMoodPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "achievements.yml");
        this.data = YamlConfiguration.loadConfiguration(dataFile);
    }

    /** Re-reads achievements.yml from disk, discarding any unsaved in-memory state. */
    public void reload() {
        this.data = YamlConfiguration.loadConfiguration(dataFile);
    }

    // -- Unlocking --

    /** Unlocks an achievement for a player if they haven't already earned it. Returns true if newly unlocked. */
    public boolean unlock(Player player, String achievementId) {
        String path = unlockedPath(player) + achievementId;
        if (data.contains(path)) {
            return false;
        }

        data.set(path, System.currentTimeMillis());
        scheduleSave();

        Achievement ach = ALL_ACHIEVEMENTS.stream()
                .filter(a -> a.id().equals(achievementId))
                .findFirst().orElse(null);

        if (ach != null) {
            plugin.getMessageService().sendChat(player, "§6✨ Achievement Unlocked! §e" + ach.icon() + " " + ach.name());
            plugin.getMessageService().sendChat(player, "§7  " + ach.description());
            plugin.getPlayerJournalService().addEntry(player, "Achievement: " + ach.icon() + " " + ach.name());
        }

        return true;
    }

    public boolean hasUnlocked(Player player, String achievementId) {
        return data.contains(unlockedPath(player) + achievementId);
    }

    /** Returns a map of unlocked achievement id -> unlock timestamp for the player. */
    public Map<String, Long> getPlayerAchievements(Player player) {
        Map<String, Long> result = new LinkedHashMap<>();
        ConfigurationSection section = data.getConfigurationSection(unlockedPath(player));
        if (section == null) return result;
        for (String id : section.getKeys(false)) {
            result.put(id, section.getLong(id));
        }
        return result;
    }

    public int getUnlockedCount(Player player) {
        return getPlayerAchievements(player).size();
    }

    // -- Progress events --

    /** Called on every scaled-mob kill. Tracks cumulative kills for the slayer tiers and checks danger_seeker directly. */
    public void onScaledMobKill(Player killer, int level) {
        int count = incrementProgress(killer, "scaled_kills");
        for (String id : achievementsForCount(count, new int[]{10, 100, 500},
                new String[]{"slayer_initiate", "slayer_veteran", "slayer_legend"})) {
            unlock(killer, id);
        }
        if (level >= 30) {
            unlock(killer, "danger_seeker");
        }
    }

    /** Called on every confirmed zone change. Tracks distinct zones for first_steps/world_explorer/seasoned_traveler and checks void_walker. */
    public void onZoneVisited(Player player, String zoneKey) {
        String path = progressPath(player) + "zones_visited";
        List<String> visited = new ArrayList<>(data.getStringList(path));
        if (!visited.contains(zoneKey)) {
            visited.add(zoneKey);
            data.set(path, visited);
            scheduleSave();

            for (String id : achievementsForCount(visited.size(), new int[]{1, 10, 25},
                    new String[]{"first_steps", "world_explorer", "seasoned_traveler"})) {
                unlock(player, id);
            }
        }

        if (player.getWorld().getEnvironment() == World.Environment.THE_END) {
            unlock(player, "void_walker");
        }
    }

    /** Called on every crop harvest. Tracks cumulative harvests for farmer and checks master_farmer directly. */
    public void onCropHarvested(Player player, CropQuality quality) {
        int count = incrementProgress(player, "crop_harvests");
        for (String id : achievementsForCount(count, new int[]{50}, new String[]{"farmer"})) {
            unlock(player, id);
        }
        if (quality == CropQuality.GOLD) {
            unlock(player, "master_farmer");
        }
    }

    /** Called after a player newly discovers a recipe. Compares against the total recipe count for chef/gourmet. */
    public void onRecipeDiscovered(Player player, int discoveredCount, int totalRecipes) {
        for (String id : achievementsForCount(discoveredCount, new int[]{10}, new String[]{"chef"})) {
            unlock(player, id);
        }
        if (totalRecipes > 0 && discoveredCount >= totalRecipes) {
            unlock(player, "gourmet");
        }
    }

    /** Called once per in-game day (24000 ticks) from SeasonManager.advanceDay(). Ticks night/survival counters for all online players. */
    public void onGlobalDayPassed() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            int nights = incrementProgress(player, "nights_survived");
            for (String id : achievementsForCount(nights, new int[]{30}, new String[]{"night_owl"})) {
                unlock(player, id);
            }

            int daysSinceDeath = incrementProgress(player, "days_since_death");
            for (String id : achievementsForCount(daysSinceDeath, new int[]{24}, new String[]{"immortal"})) {
                unlock(player, id);
            }
        }
    }

    /** Resets the immortal-streak counter for a player. Called on death. */
    public void resetDeathStreak(Player player) {
        data.set(progressPath(player) + "days_since_death", 0);
        scheduleSave();
    }

    // -- Helpers --

    private String unlockedPath(Player player) {
        return "players." + player.getUniqueId() + ".unlocked.";
    }

    private String progressPath(Player player) {
        return "players." + player.getUniqueId() + ".progress.";
    }

    private int incrementProgress(Player player, String key) {
        String path = progressPath(player) + key;
        int value = data.getInt(path, 0) + 1;
        data.set(path, value);
        scheduleSave();
        return value;
    }

    /** Pure helper: returns the ids whose threshold {@code count} has reached or passed. Public+static for unit testing without Bukkit. */
    static List<String> achievementsForCount(int count, int[] thresholds, String[] ids) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < thresholds.length; i++) {
            if (count >= thresholds[i]) {
                result.add(ids[i]);
            }
        }
        return result;
    }

    private void scheduleSave() {
        if (saveScheduled) {
            return;
        }
        saveScheduled = true;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            saveScheduled = false;
            save();
        }, SAVE_DELAY_TICKS);
    }

    private void save() {
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save achievements.yml: " + e.getMessage());
        }
    }
}
