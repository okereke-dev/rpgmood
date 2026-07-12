package com.ricardo.rpgmood;

import com.ricardo.rpgmood.farming.CropQuality;
import com.ricardo.rpgmood.farming.SeasonManager;
import com.ricardo.rpgmood.farming.animal.AnimalType;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tracks and persists player achievements related to exploration, combat, farming, survival,
 * and (softly) RPGLoot loot. Unlock state and progress counters are stored in achievements.yml,
 * debounced-saved like {@link PlayerStatsService} since combat/farming progress can be written
 * on every kill or harvest.
 */
public class AchievementManager {

    public enum Category { EXPLORATION, COMBAT, SURVIVAL, FARMING, LOOT }

    public record Achievement(String id, String name, String description, String icon, Category category) {}

    /** Ordered (not a Set) so display order is stable rather than JVM-dependent. */
    public static final List<Achievement> ALL_ACHIEVEMENTS = List.of(
            // -- Exploration --
            new Achievement("first_steps", "First Steps", "Enter your first zone", "🚶", Category.EXPLORATION),
            new Achievement("world_explorer", "World Explorer", "Discover 10 different zones", "🗺️", Category.EXPLORATION),
            new Achievement("seasoned_traveler", "Seasoned Traveler", "Discover 25 different zones", "🌍", Category.EXPLORATION),
            new Achievement("void_walker", "Void Walker", "Survive the End", "🌌", Category.EXPLORATION),
            new Achievement("cartographer", "Cartographer", "Discover every zone defined in zones.yml", "🧭", Category.EXPLORATION),
            // -- Combat --
            new Achievement("slayer_initiate", "Slayer Initiate", "Kill 10 scaled mobs", "⚔️", Category.COMBAT),
            new Achievement("slayer_veteran", "Slayer Veteran", "Kill 100 scaled mobs", "🗡️", Category.COMBAT),
            new Achievement("slayer_legend", "Slayer Legend", "Kill 500 scaled mobs", "👑", Category.COMBAT),
            new Achievement("danger_seeker", "Danger Seeker", "Kill a level 30+ mob", "🔥", Category.COMBAT),
            new Achievement("untouchable", "Untouchable", "Kill a level 20+ mob without taking damage from it", "🥷", Category.COMBAT),
            new Achievement("godslayer", "Godslayer", "Kill the Warden, Wither, Elder Guardian, and Ender Dragon", "🏆", Category.COMBAT),
            new Achievement("overkill", "Overkill", "Kill a level 30+ mob that drops a Legendary item", "🎯", Category.COMBAT),
            // -- Survival --
            new Achievement("immortal", "Immortal", "Survive 24 MC days without dying", "⭐", Category.SURVIVAL),
            new Achievement("night_owl", "Night Owl", "Survive 30 nights", "🦉", Category.SURVIVAL),
            new Achievement("storm_chaser", "Storm Chaser", "Get struck by lightning and survive", "🌪️", Category.SURVIVAL),
            new Achievement("death_tourist", "Death Tourist", "Die in 8 different biomes", "💀", Category.SURVIVAL),
            // -- Farming --
            new Achievement("farmer", "Farmer", "Harvest 50 crops", "🌾", Category.FARMING),
            new Achievement("master_farmer", "Master Farmer", "Harvest a Gold-quality crop", "🌟", Category.FARMING),
            new Achievement("chef", "Chef", "Cook 10 recipes", "🍳", Category.FARMING),
            new Achievement("gourmet", "Gourmet", "Cook all recipes", "👨‍🍳", Category.FARMING),
            new Achievement("seasons_first", "First of the Season", "Witness a season change", "🌸", Category.FARMING),
            new Achievement("four_seasons", "Four Seasons", "Witness all 4 seasons change", "🌈", Category.FARMING),
            new Achievement("noahs_ark", "Noah's Ark", "Own one of every animal type", "🐄", Category.FARMING),
            new Achievement("beloved", "Beloved", "Get any animal to max affection", "💕", Category.FARMING),
            new Achievement("green_thumb", "Green Thumb", "Harvest a Gold-quality crop of every crop type", "🏵️", Category.FARMING),
            // -- Loot (RPGLoot integration — unreachable if RPGLoot isn't installed) --
            new Achievement("legendary_hunter", "Legendary Hunter", "Obtain your first Legendary-rarity item", "💎", Category.LOOT),
            new Achievement("relic_bearer", "Relic Bearer", "Obtain your first Artifact", "🗝️", Category.LOOT),
            new Achievement("godslayers_arsenal", "Godslayer's Arsenal", "Obtain all 4 Artifacts", "🔱", Category.LOOT),
            new Achievement("matching_set", "Matching Set", "Have a full 5-piece set active", "🎽", Category.LOOT),
            new Achievement("head_to_toe", "Head to Toe", "Have a full 5-piece Legendary set active", "💫", Category.LOOT),
            new Achievement("dressed_to_kill", "Dressed to Kill", "Have Legendary gear in all 5 equipment slots", "🛡️", Category.LOOT)
    );

    private static final Set<String> BOSS_TYPES = Set.of("WARDEN", "WITHER", "ELDER_GUARDIAN", "ENDER_DRAGON");
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

    /** Called when a level 30+ scaled mob's drops are finalized. Unlocks overkill if a Legendary RPGLoot item is among them. */
    public void onScaledMobKillWithDrops(Player killer, int level, List<ItemStack> drops) {
        if (level < 30 || drops == null) return;
        for (ItemStack drop : drops) {
            if ("LEGENDARY".equals(RPGLootIntegration.getRarity(drop))) {
                unlock(killer, "overkill");
                return;
            }
        }
    }

    /** Called when a level 20+ scaled mob is killed without ever having damaged its killer. */
    public void onUndamagedKill(Player killer, int level) {
        if (level >= 20) {
            unlock(killer, "untouchable");
        }
    }

    /** Called on any kill of one of the four major bosses, scaled or not. Tracks distinct bosses for godslayer. */
    public void onBossKilled(Player killer, EntityType type) {
        if (!BOSS_TYPES.contains(type.name())) return;
        List<String> bossesKilled = addToProgressSet(killer, "bosses_killed", type.name());
        if (bossesKilled.size() >= BOSS_TYPES.size()) {
            unlock(killer, "godslayer");
        }
    }

    /** Called on every confirmed zone change. Tracks distinct zones for first_steps/world_explorer/seasoned_traveler/cartographer and checks void_walker. */
    public void onZoneVisited(Player player, String zoneKey) {
        List<String> visited = addToProgressSet(player, "zones_visited", zoneKey);

        for (String id : achievementsForCount(visited.size(), new int[]{1, 10, 25},
                new String[]{"first_steps", "world_explorer", "seasoned_traveler"})) {
            unlock(player, id);
        }

        ConfigurationSection zonesSection = plugin.getConfigManager().getZones().getConfigurationSection("zones");
        if (zonesSection != null) {
            Set<String> configuredZones = zonesSection.getKeys(false);
            if (!configuredZones.isEmpty() && visited.containsAll(configuredZones)) {
                unlock(player, "cartographer");
            }
        }

        if (player.getWorld().getEnvironment() == World.Environment.THE_END) {
            unlock(player, "void_walker");
        }
    }

    /** Called on every crop harvest. Tracks cumulative harvests for farmer, and distinct Gold-quality crop types for green_thumb. */
    public void onCropHarvested(Player player, String cropType, CropQuality quality) {
        int count = incrementProgress(player, "crop_harvests");
        for (String id : achievementsForCount(count, new int[]{50}, new String[]{"farmer"})) {
            unlock(player, id);
        }
        if (quality == CropQuality.GOLD) {
            unlock(player, "master_farmer");
            List<String> goldTypes = addToProgressSet(player, "gold_crop_types", cropType);
            int totalCropTypes = plugin.getCropManager().getCropDefinitions().size();
            if (totalCropTypes > 0 && goldTypes.size() >= totalCropTypes) {
                unlock(player, "green_thumb");
            }
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

    /** Called when a player claims (befriends or breeds) an animal. Tracks distinct animal types owned for noahs_ark. */
    public void onAnimalClaimed(Player player, AnimalType type) {
        List<String> owned = addToProgressSet(player, "animal_types_owned", type.name());
        if (owned.size() >= AnimalType.values().length) {
            unlock(player, "noahs_ark");
        }
    }

    /** Called whenever an animal's affection changes. Unlocks beloved if it reached max (5 hearts). */
    public void onAnimalMaxAffection(Player player) {
        unlock(player, "beloved");
    }

    /** Called once per season change from SeasonManager.advanceDay(). Unlocks seasons_first and tracks distinct seasons for four_seasons. */
    public void onSeasonChanged(SeasonManager.Season season) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            unlock(player, "seasons_first");
            List<String> seasons = addToProgressSet(player, "seasons_witnessed", season.name());
            if (seasons.size() >= SeasonManager.Season.values().length) {
                unlock(player, "four_seasons");
            }
        }
    }

    /** Called after a lightning strike damages a player who survives it. */
    public void onLightningSurvived(Player player) {
        unlock(player, "storm_chaser");
    }

    /** Called on player death. Tracks distinct death biomes for death_tourist. */
    public void onPlayerDeathInBiome(Player player, String biomeKey) {
        List<String> biomes = addToProgressSet(player, "death_biomes", biomeKey);
        if (biomes.size() >= 8) {
            unlock(player, "death_tourist");
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

    // -- RPGLoot integration (soft — no-ops harmlessly if RPGLoot isn't installed) --

    /** Called when a player picks up or equips an item; reads RPGLoot's PDC tags if present. */
    public void onRPGLootItemFound(Player player, ItemStack item) {
        if (item == null) return;

        if ("LEGENDARY".equals(RPGLootIntegration.getRarity(item))) {
            unlock(player, "legendary_hunter");
        }

        String artifactId = RPGLootIntegration.getArtifactId(item);
        if (artifactId != null) {
            unlock(player, "relic_bearer");
            List<String> found = addToProgressSet(player, "artifacts_found", artifactId);
            if (found.size() >= 4) {
                unlock(player, "godslayers_arsenal");
            }
        }
    }

    /** Called on armor/held-item changes. Checks RPGLoot's active-set state and a full-Legendary loadout. */
    public void onEquipmentChanged(Player player) {
        String activeSetRarity = RPGLootIntegration.getActiveSetRarity(player);
        if (activeSetRarity != null) {
            unlock(player, "matching_set");
            if ("LEGENDARY".equals(activeSetRarity)) {
                unlock(player, "head_to_toe");
            }
        }

        PlayerInventory inv = player.getInventory();
        ItemStack[] slots = {inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots(), inv.getItemInMainHand()};
        boolean allLegendary = true;
        for (ItemStack item : slots) {
            if (!"LEGENDARY".equals(RPGLootIntegration.getRarity(item))) {
                allLegendary = false;
                break;
            }
        }
        if (allLegendary) {
            unlock(player, "dressed_to_kill");
        }
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

    /** Adds a value to a persisted per-player string set if not already present. Returns the resulting set (a copy). */
    private List<String> addToProgressSet(Player player, String key, String value) {
        String path = progressPath(player) + key;
        List<String> set = new ArrayList<>(data.getStringList(path));
        if (!set.contains(value)) {
            set.add(value);
            data.set(path, set);
            scheduleSave();
        }
        return set;
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
