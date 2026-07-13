package com.okereke.rpgmood.farming;

import com.okereke.rpgmood.RPGMoodPlugin;
import com.okereke.rpgmood.ZoneManager;
import com.okereke.rpgmood.api.PlayerCropHarvestEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Manages crop growth, quality calculation, and harvest.
 * Quality factors: soil fertility, water proximity, rain, season multiplier, zone danger level.
 */
public class CropManager {

    private static final int WATER_RADIUS = 5;
    private static final int STRUCTURE_CACHE_MAX_SIZE = 256;

    private final RPGMoodPlugin plugin;
    private final Map<String, CropDefinition> cropDefinitions = new HashMap<>();

    /** Cache for soil fertility by biome group key. */
    private final Map<String, Double> fertilityCache = new HashMap<>();

    public CropManager(RPGMoodPlugin plugin) {
        this.plugin = plugin;
        loadCropDefinitions();
        loadFertilityCache();
    }

    // -- Crop definitions from farming.yml

    public record CropDefinition(
            String id,
            Material material,
            int growthStages,
            int baseYield,
            Material seed,
            List<String> seasons
    ) {}

    private void loadCropDefinitions() {
        cropDefinitions.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("farming.crops");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String path = "farming.crops." + key;
            cropDefinitions.put(key, new CropDefinition(
                    key,
                    Material.getMaterial(section.getString(key + ".material", "WHEAT")),
                    section.getInt(key + ".growth_stages", 4),
                    section.getInt(key + ".base_yield", 1),
                    Material.getMaterial(section.getString(key + ".seed", "WHEAT_SEEDS")),
                    section.getStringList(key + ".seasons")
            ));
        }
    }

    private void loadFertilityCache() {
        fertilityCache.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("farming.soil_fertility");
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            fertilityCache.put(key, section.getDouble(key, 0.5));
        }
    }

    public void reload() {
        loadCropDefinitions();
        loadFertilityCache();
    }

    public Map<String, CropDefinition> getCropDefinitions() {
        return Map.copyOf(cropDefinitions);
    }

    // -- Quality calculation

    /**
     * Calculates the crop quality based on environmental factors.
     * Returns a score 0.0–1.0 which maps to BRONZE, SILVER, or GOLD.
     */
    public double calculateQualityScore(Location location, Player player) {
        String biomeGroup = ZoneManager.normalizeBiomeGroup(
                location.getBlock().getBiome().name().toUpperCase(Locale.ROOT));

        // 1. Base soil fertility (0.0 - 1.0)
        double fertility = fertilityCache.getOrDefault(biomeGroup, 0.5);

        // 2. Water bonus (+0.25 if water within radius)
        double waterBonus = hasWaterNearby(location) ? plugin.getConfig().getDouble("farming.water_bonus", 0.25) : 0.0;

        // 3. Season multiplier (Spring=1.5, Summer=2.0, Autumn=1.0, Winter=0.3 applied as bonus)
        double seasonMultiplier = plugin.getSeasonManager().getGrowthMultiplier();
        // Normalize season contribution: (multiplier - 1.0) * 0.2 => +0.1 spring, +0.2 summer, 0 autumn, -0.14 winter
        double seasonContribution = (seasonMultiplier - 1.0) * 0.2;

        // 4. Rain check — rain hydrates crops naturally
        double rainBonus = 0.0;
        if (location.getWorld() != null && location.getWorld().hasStorm()) {
            rainBonus = 0.1;
        }

        // 5. Danger level of zone (more danger = more fertile due to magical energy)
        double dangerBonus = 0.0;
        if (player != null) {
            int dangerLevel = plugin.getMobScalingService().calculateLevelAt(
                    location,
                    plugin.getMobScalingService().getBaseLevel(org.bukkit.entity.EntityType.ZOMBIE));
            dangerBonus = Math.min(0.15, dangerLevel * 0.005); // up to +0.15 at level 30+
        }

        double score = fertility + waterBonus + seasonContribution + rainBonus + dangerBonus;

        // Clamp between 0.1 and 0.95 (never perfect automatically, never 0)
        return Math.max(0.1, Math.min(0.95, score));
    }

    /** Maps a quality score to a CropQuality tier using farming.yml thresholds. */
    public CropQuality scoreToQuality(double score) {
        double bronzeMax = plugin.getConfig().getDouble("farming.quality.bronze_max", 0.4);
        double silverMax = plugin.getConfig().getDouble("farming.quality.silver_max", 0.7);

        if (score > silverMax) return CropQuality.GOLD;
        if (score > bronzeMax) return CropQuality.SILVER;
        return CropQuality.BRONZE;
    }

    // -- Harvest

    /**
     * Called when a player harvests a mature crop.
     * Calculates quality, generates drops, fires API event.
     */
    public List<ItemStack> harvestCrop(Block block, Player player) {
        CropDefinition def = findCropDefinition(block);
        if (def == null) return List.of();

        // Calculate quality
        double score = calculateQualityScore(block.getLocation(), player);
        CropQuality quality = scoreToQuality(score);

        // Generate drops
        List<ItemStack> drops = generateHarvestDrops(def, quality, player);

        // Fire API event
        PlayerCropHarvestEvent event = new PlayerCropHarvestEvent(
                player, def.id(), quality, block.getLocation(), drops);
        Bukkit.getPluginManager().callEvent(event);

        // Add journal entry for gold quality
        if (quality == CropQuality.GOLD) {
            plugin.getPlayerJournalService().addEntry(player,
                    "§e\u2728 Harvested Gold-quality " + def.id() + "!");
        }

        plugin.getAchievementManager().onCropHarvested(player, def.id(), quality);

        return event.getDrops();
    }

    /** Generates the item drops for a harvested crop based on quality. */
    private List<ItemStack> generateHarvestDrops(CropDefinition def, CropQuality quality, Player player) {
        List<ItemStack> drops = new ArrayList<>();

        double bonusMult = quality.getBonusMultiplier(); // Bronze=1.0, Silver=1.5, Gold=2.0
        int yield = (int) Math.max(1, Math.round(def.baseYield() * bonusMult));

        // Main crop item
        drops.add(new ItemStack(def.material(), yield));

        // Seeds (bonus chance based on quality)
        int seedCount = quality.getTier();
        drops.add(new ItemStack(def.seed(), seedCount));

        return drops;
    }

    // -- Growth helpers

    /** Checks if the block is one of our tracked crops. */
    public boolean isTrackedCrop(Block block) {
        return findCropDefinition(block) != null;
    }

    /** Checks if the crop is fully mature (ready to harvest). */
    public boolean isMature(Block block) {
        BlockData data = block.getBlockData();
        if (data instanceof Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        return true;
    }

    /** Finds the crop definition matching this block's material. */
    private CropDefinition findCropDefinition(Block block) {
        Material type = block.getType();
        for (CropDefinition def : cropDefinitions.values()) {
            if (def.material() == type) {
                return def;
            }
        }
        return null;
    }

    /** Checks if there's water within the configured radius of the location. */
    private boolean hasWaterNearby(Location location) {
        int radius = plugin.getConfig().getInt("farming.water_radius", WATER_RADIUS);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    Block relative = location.getBlock().getRelative(dx, dy, dz);
                    if (relative.getType() == Material.WATER) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Checks if a seed item matches a crop definition's seed. */
    public boolean isSeed(Material material) {
        for (CropDefinition def : cropDefinitions.values()) {
            if (def.seed() == material) {
                return true;
            }
        }
        return false;
    }

    /** Gets the crop definition associated with a seed material. */
    public CropDefinition getCropBySeed(Material seed) {
        for (CropDefinition def : cropDefinitions.values()) {
            if (def.seed() == seed) {
                return def;
            }
        }
        return null;
    }
}
