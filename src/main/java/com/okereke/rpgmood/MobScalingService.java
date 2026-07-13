package com.okereke.rpgmood;

import com.okereke.rpgmood.api.MobScaleEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.StructureType;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MobScalingService {

    /** Grid cell size (blocks) used to cache structure-proximity lookups; larger values reduce scan frequency at the cost of coarser granularity. */
    private static final int STRUCTURE_CACHE_GRID_SIZE = 512;
    private static final int STRUCTURE_CACHE_MAX_ENTRIES = 4096;

    /**
     * PDC key storing the mob's scaled level as an Integer, e.g. for other plugins (RPGLoot) to read
     * and scale drop rarity by difficulty. Namespaced under this plugin, so it's readable by anyone as
     * "rpgmood:level" without a hard dependency on RPGMood.
     */
    private final NamespacedKey levelKey;

    private final RPGMoodPlugin plugin;
    private final Map<String, Integer> structureBonusCache = new LinkedHashMap<>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
            return size() > STRUCTURE_CACHE_MAX_ENTRIES;
        }
    };

    /** Maps variant/child biome names to their parent biome group for bonus lookups (mirrors ZoneManager's grouping). */
    private static final Map<String, String> BIOME_NORMALIZATION = new HashMap<>();

    static {
        BIOME_NORMALIZATION.put("SUNFLOWER_PLAINS", "PLAINS");
        BIOME_NORMALIZATION.put("MEADOW", "PLAINS");
        BIOME_NORMALIZATION.put("FLOWER_FOREST", "FOREST");
        BIOME_NORMALIZATION.put("FOREST", "FOREST");
        BIOME_NORMALIZATION.put("BIRCH_FOREST", "FOREST");
        BIOME_NORMALIZATION.put("OLD_GROWTH_BIRCH_FOREST", "FOREST");
        BIOME_NORMALIZATION.put("OLD_GROWTH_PINE_TAIGA", "SNOWY_TAIGA");
        BIOME_NORMALIZATION.put("OLD_GROWTH_SPRUCE_TAIGA", "SNOWY_TAIGA");
        BIOME_NORMALIZATION.put("GIANT_TREE_TAIGA", "TAIGA");
        BIOME_NORMALIZATION.put("GIANT_SPRUCE_TAIGA", "TAIGA");
        BIOME_NORMALIZATION.put("ICE_SPIKES", "SNOWY_TAIGA");
        BIOME_NORMALIZATION.put("SNOWY_PLAINS", "SNOWY_TAIGA");
        BIOME_NORMALIZATION.put("SNOWY_MOUNTAINS", "MOUNTAINS");
        BIOME_NORMALIZATION.put("FROZEN_PEAKS", "MOUNTAINS");
        BIOME_NORMALIZATION.put("JAGGED_PEAKS", "MOUNTAINS");
        BIOME_NORMALIZATION.put("GROVE", "DARK_FOREST");
        BIOME_NORMALIZATION.put("BAMBOO_JUNGLE", "JUNGLE");
        BIOME_NORMALIZATION.put("MANGROVE_SWAMP", "SWAMP");
        BIOME_NORMALIZATION.put("CRIMSON_FOREST", "NETHER_CRIMSON");
        BIOME_NORMALIZATION.put("WARPED_FOREST", "NETHER_WARPED");
        BIOME_NORMALIZATION.put("BASALT_DELTAS", "NETHER_BASALT");
        BIOME_NORMALIZATION.put("SOUL_SAND_VALLEY", "NETHER_SOUL");
        BIOME_NORMALIZATION.put("NETHER_WASTES", "NETHER_WASTES");
        BIOME_NORMALIZATION.put("OCEAN", "OCEAN");
        BIOME_NORMALIZATION.put("LUKEWARM_OCEAN", "OCEAN");
        BIOME_NORMALIZATION.put("WARM_OCEAN", "OCEAN");
        BIOME_NORMALIZATION.put("DEEP_OCEAN", "OCEAN");
        BIOME_NORMALIZATION.put("COLD_OCEAN", "OCEAN");
        BIOME_NORMALIZATION.put("FROZEN_OCEAN", "OCEAN");
        BIOME_NORMALIZATION.put("RIVER", "RIVER");
        BIOME_NORMALIZATION.put("FROZEN_RIVER", "RIVER");
        BIOME_NORMALIZATION.put("BEACH", "BEACH");
        BIOME_NORMALIZATION.put("STONE_SHORE", "BEACH");
        BIOME_NORMALIZATION.put("WARM_BEACH", "BEACH");
        BIOME_NORMALIZATION.put("SNOWY_BEACH", "BEACH");
        BIOME_NORMALIZATION.put("MUSHROOM_FIELDS", "MUSHROOM");
        BIOME_NORMALIZATION.put("MUSHROOM_FIELD_SHORE", "MUSHROOM");
        BIOME_NORMALIZATION.put("THE_END", "END");
        BIOME_NORMALIZATION.put("END_MIDLANDS", "END");
        BIOME_NORMALIZATION.put("END_HIGHLANDS", "END");
        BIOME_NORMALIZATION.put("END_BARRENS", "END");
        BIOME_NORMALIZATION.put("SMALL_END_ISLANDS", "END");
    }

    public MobScalingService(RPGMoodPlugin plugin) {
        this.plugin = plugin;
        this.levelKey = new NamespacedKey(plugin, "level");
    }

    public NamespacedKey getLevelKey() {
        return levelKey;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("mob_scaling.enabled", true);
    }

    public boolean shouldScale(LivingEntity entity) {
        if (!isEnabled()) {
            return false;
        }
        if (!plugin.getConfig().getBoolean("mob_scaling.hostile-only", true)) {
            return true;
        }
        return entity instanceof Monster;
    }

    /** Applies scaling to the entity and returns the level applied, or -1 if it was skipped/not scaled. */
    public int applyScaling(LivingEntity entity) {
        if (entity == null || !shouldScale(entity) || entity.getScoreboardTags().contains("rpgmood_scaled")) {
            return -1;
        }

        int level = calculateLevel(entity);
        if (level <= 0) {
            return -1;
        }

        // Fire MobScaleEvent — other plugins can modify or cancel scaling
        MobScaleEvent scaleEvent = new MobScaleEvent(entity, level);
        Bukkit.getPluginManager().callEvent(scaleEvent);
        if (scaleEvent.isCancelled()) {
            return -1;
        }
        level = scaleEvent.getLevel();

        double statMultiplier = calculateStatMultiplier(level,
                plugin.getConfig().getDouble("mob_scaling.early_game_fraction", 0.85),
                plugin.getConfig().getInt("mob_scaling.parity_level", 8));
        double armor = Math.max(0.0, (level - 1) * plugin.getConfig().getDouble("mob_scaling.armor_per_level", 0.25));
        double speedBonus = Math.max(0.0, (level - 1) * plugin.getConfig().getDouble("mob_scaling.speed_per_level", 0.0015));

        // Multiplier is relative to each mob's own vanilla baseline (getDefaultValue()), not a
        // flat constant — an Enderman scales off its own 40 HP, a Zombie off its own 20, etc.
        // Health and damage share the same curve so a scaled mob doesn't end up tanky-but-weak.
        AttributeInstance maxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            double health = Math.max(4.0, maxHealth.getDefaultValue() * statMultiplier);
            maxHealth.setBaseValue(health);
            entity.setHealth(Math.min(entity.getHealth(), health));
        }

        AttributeInstance damageAttribute = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (damageAttribute != null) {
            double damage = Math.max(0.1, damageAttribute.getDefaultValue() * statMultiplier);
            damageAttribute.setBaseValue(damage);
        }

        AttributeInstance armorAttribute = entity.getAttribute(Attribute.GENERIC_ARMOR);
        if (armorAttribute != null) {
            armorAttribute.setBaseValue(Math.max(0.0, armor));
        }

        AttributeInstance speedAttribute = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.setBaseValue(Math.max(0.01, speedAttribute.getBaseValue() + speedBonus));
        }

        entity.addScoreboardTag("rpgmood_scaled");
        entity.getPersistentDataContainer().set(levelKey, PersistentDataType.INTEGER, level);

        // Only set a custom name when the floating tag is actually shown — visual level
        // indication otherwise comes from MobAuraEffect (particle auras) and the affix
        // system's action-bar warning. Setting CustomName even while invisible would still
        // make vanilla log a "Named entity X died: ..." line on every kill (vanilla logs
        // deaths of any entity with a custom name, regardless of setCustomNameVisible), which
        // floods the console once most mobs on the server are scaled.
        boolean showNameTags = plugin.getConfig().getBoolean("mob_scaling.show_name_tags", false);
        if (showNameTags) {
            String displayName = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("mob_scaling.name_format", "&cLv. {level} {name}")
                    .replace("{level}", String.valueOf(level))
                    .replace("{name}", entity.getName()));
            entity.setCustomName(displayName);
            entity.setCustomNameVisible(true);
        }
        return level;
    }

    /** Clears the structure-bonus cache so it is rebuilt on the next lookup. Call after a config reload. */
    public void invalidateStructureCache() {
        structureBonusCache.clear();
    }

    public int calculateLevel(LivingEntity entity) {
        return calculateLevelAt(entity.getLocation(), getBaseLevel(entity.getType()));
    }

    /** Computes the scaling level a mob with the given base level would receive at this location right now. */
    public int calculateLevelAt(Location location, int baseLevel) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("mob_scaling");
        if (section == null) {
            return 1;
        }

        double distanceFromSpawn = location.distance(location.getWorld().getSpawnLocation());
        int biomeBonus = getBiomeBonus(location);
        int structureBonus = getStructureBonus(location);
        int nearbyPlayers = countNearbyPlayers(location);

        return calculateDifficultyLevel(
                baseLevel,
                distanceFromSpawn,
                biomeBonus,
                structureBonus,
                nearbyPlayers,
                section.getInt("max-level", 40),
                section.getDouble("radius.step_distance", 180.0),
                section.getInt("players_bonus_per_player", 1)
        );
    }

    public static int calculateDifficultyLevel(int baseLevel, double distanceFromSpawn, int biomeBonus, int structureBonus, int nearbyPlayers, int maxLevel, double stepDistance, int playerBonusPerPlayer) {
        int radialLevel = (int) Math.floor(distanceFromSpawn / stepDistance);
        int adjustedBase = Math.max(0, baseLevel - 2);
        int total = adjustedBase + Math.max(0, radialLevel - 1) + biomeBonus + structureBonus + nearbyPlayers * playerBonusPerPlayer;
        return Math.max(1, Math.min(maxLevel, total));
    }

    /**
     * Fraction of a mob's own vanilla stat it gets at this level — shared by health and damage
     * so both stay in lockstep instead of drifting to different "parity" levels. At level 1,
     * the mob is at {@code earlyGameFraction} of vanilla; at {@code parityLevel}, exactly 1.0
     * (vanilla-equivalent); beyond that it keeps climbing at the same linear rate.
     */
    public static double calculateStatMultiplier(int level, double earlyGameFraction, int parityLevel) {
        double perLevelGrowth = (1.0 - earlyGameFraction) / Math.max(1, parityLevel - 1);
        return earlyGameFraction + (level - 1) * perLevelGrowth;
    }

    public int getBaseLevel(EntityType type) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("mob_scaling.base-levels");
        if (section == null) {
            return 1;
        }

        String key = type.name();
        return section.getInt(key, 1);
    }

    private int getBiomeBonus(Location location) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("mob_scaling.biome-bonuses");
        if (section == null) {
            return 0;
        }

        String rawBiome = location.getBlock().getBiome().name();
        String normalized = BIOME_NORMALIZATION.getOrDefault(rawBiome, rawBiome);
        return section.getInt(normalized, 0);
    }

    private int getStructureBonus(Location location) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("mob_scaling.structure-bonuses");
        if (section == null) {
            return 0;
        }

        if (!location.getWorld().isChunkGenerated(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return 0;
        }

        int gridX = Math.floorDiv(location.getBlockX(), STRUCTURE_CACHE_GRID_SIZE);
        int gridZ = Math.floorDiv(location.getBlockZ(), STRUCTURE_CACHE_GRID_SIZE);
        String cacheKey = location.getWorld().getName() + "|" + gridX + "|" + gridZ;

        Integer cached = structureBonusCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        int bonus = scanStructureBonus(location, section);
        structureBonusCache.put(cacheKey, bonus);
        return bonus;
    }

    private int scanStructureBonus(Location location, ConfigurationSection section) {
        long startTime = System.nanoTime();
        final long TIMEOUT_NANOS = 100_000L; // 100 microseconds max per scan

        for (String key : section.getKeys(false)) {
            if (key == null || key.isBlank()) continue;

            // Timeout: if we've spent too long, skip structure bonus
            if (System.nanoTime() - startTime > TIMEOUT_NANOS) {
                return 0;
            }

            String normalizedKey = key.toLowerCase(Locale.ROOT).trim();
            if (normalizedKey.isBlank()) continue;

            NamespacedKey namespacedKey;
            try {
                namespacedKey = NamespacedKey.fromString("minecraft:" + normalizedKey);
            } catch (IllegalArgumentException ex) {
                continue;
            }

            try {
                var registry = Bukkit.getRegistry(StructureType.class);
                if (registry == null) continue;
                StructureType structureType = registry.get(namespacedKey);
                if (structureType == null) continue;
                if (location.getWorld().locateNearestStructure(location, structureType, 64, false) != null) {
                    return section.getInt(key, 0);
                }
            } catch (Exception ex) {
                // Structure lookup failed — skip silently
            }
        }
        return 0;
    }

    private int countNearbyPlayers(Location location) {
        int count = 0;
        for (Player player : location.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(location) <= 900.0) {
                count++;
            }
        }
        return count;
    }
}
