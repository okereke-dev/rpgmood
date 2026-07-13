package com.okereke.rpgmood.farming.animal;

import com.okereke.rpgmood.RPGMoodPlugin;
import com.okereke.rpgmood.farming.SeasonManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central manager for all owned animals.
 * Handles CRUD, persistence, daily ticks, affection, health, and sickness.
 */
public class AnimalManager {

    private static final int AFFECTIONS_MAX_DAILY_GAIN = 5;
    private static final long SAVE_INTERVAL_TICKS = 6000L; // every 5 minutes

    private final RPGMoodPlugin plugin;
    private final File dataFile;
    private YamlConfiguration data;
    private final Map<UUID, AnimalData> animals = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private int currentServerDay = 0;

    // Blocks considered as "fence" for enclosure detection
    private static final Material[] FENCE_BLOCKS = {
            Material.OAK_FENCE, Material.OAK_FENCE_GATE,
            Material.BIRCH_FENCE, Material.BIRCH_FENCE_GATE,
            Material.SPRUCE_FENCE, Material.SPRUCE_FENCE_GATE,
            Material.JUNGLE_FENCE, Material.JUNGLE_FENCE_GATE,
            Material.DARK_OAK_FENCE, Material.DARK_OAK_FENCE_GATE,
            Material.ACACIA_FENCE, Material.ACACIA_FENCE_GATE,
            Material.CRIMSON_FENCE, Material.CRIMSON_FENCE_GATE,
            Material.WARPED_FENCE, Material.WARPED_FENCE_GATE,
            Material.NETHER_BRICK_FENCE,
            Material.COBBLESTONE_WALL, Material.MOSSY_COBBLESTONE_WALL
    };

    // Blocks considered as "roof" for enclosure
    private static final Material[] ROOF_BLOCKS = {
            Material.OAK_SLAB, Material.OAK_STAIRS, Material.OAK_PLANKS,
            Material.STONE_SLAB, Material.STONE_BRICK_SLAB,
            Material.GRASS_BLOCK, Material.DIRT, Material.COBBLESTONE
    };

    // Grass blocks for grazing detection
    private static final Material[] GRASS_BLOCKS = {
            Material.GRASS_BLOCK, Material.TALL_GRASS, Material.SHORT_GRASS,
            Material.FERN, Material.LARGE_FERN
    };

    public AnimalManager(RPGMoodPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "animals.yml");
        this.data = YamlConfiguration.loadConfiguration(dataFile);
        loadAll();
        startAutoSave();
    }

    // -- Persistence --

    private void loadAll() {
        animals.clear();
        ConfigurationSection section = data.getConfigurationSection("animals");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                UUID animalId = UUID.fromString(key);
                ConfigurationSection animalSection = section.getConfigurationSection(key);
                if (animalSection != null) {
                    animals.put(animalId, AnimalData.load(animalId, animalSection));
                }
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid animal UUID in animals.yml: " + key);
            }
        }
        plugin.getLogger().info("Loaded " + animals.size() + " animals.");
    }

    private void saveAll() {
        ConfigurationSection section = data.createSection("animals");
        for (Map.Entry<UUID, AnimalData> entry : animals.entrySet()) {
            ConfigurationSection animalSection = section.createSection(entry.getKey().toString());
            entry.getValue().save(animalSection);
        }
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save animals.yml: " + e.getMessage());
        }
    }

    public void saveAnimal(AnimalData animal) {
        ConfigurationSection section = data.createSection("animals." + animal.getAnimalId().toString());
        animal.save(section);
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save animal " + animal.getAnimalId() + ": " + e.getMessage());
        }
    }

    private void startAutoSave() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::saveAll, SAVE_INTERVAL_TICKS, SAVE_INTERVAL_TICKS);
    }

    public void reload() {
        this.data = YamlConfiguration.loadConfiguration(dataFile);
        loadAll();
    }

    // -- Animal CRUD --

    /** Registers a new animal in the system. */
    public AnimalData registerAnimal(UUID entityId, AnimalType type, UUID ownerId, String name) {
        AnimalData data = new AnimalData(entityId, type, ownerId, name);
        animals.put(entityId, data);
        saveAnimal(data);
        return data;
    }

    /** Removes an animal from tracking (death, sold). */
    public void unregisterAnimal(UUID entityId) {
        animals.remove(entityId);
        data.set("animals." + entityId.toString(), null);
        try { data.save(dataFile); } catch (IOException ignored) {}
    }

    /** Gets animal data by entity UUID. */
    public AnimalData getAnimal(UUID entityId) {
        return animals.get(entityId);
    }

    /** Gets all animals owned by a player. */
    public List<AnimalData> getOwnedAnimals(UUID ownerId) {
        List<AnimalData> result = new ArrayList<>();
        for (AnimalData animal : animals.values()) {
            if (animal.getOwnerId().equals(ownerId)) {
                result.add(animal);
            }
        }
        return result;
    }

    /** Gets all registered animals. */
    public Map<UUID, AnimalData> getAllAnimals() {
        return Map.copyOf(animals);
    }

    // -- Daily Tick (called once per MC day, tick 0) --

    public void dailyTick() {
        currentServerDay++;
        SeasonManager.Season season = plugin.getSeasonManager().getCurrentSeason();
        boolean isWinter = season == SeasonManager.Season.WINTER;

        for (AnimalData animal : animals.values()) {
            // Find the living entity
            Entity entity = findEntity(animal.getAnimalId());
            if (entity == null || entity.isDead()) continue;

            // Detect if inside enclosure
            boolean inside = detectEnclosure(entity.getLocation());
            animal.setInside(inside);

            // Apply affection penalties
            applyDailyPenalties(animal, isWinter);

            // Check sickness
            checkSickness(animal);

            // Check death
            checkDeath(animal, entity);

            // Save changes
            saveAnimal(animal);
        }
    }

    private void applyDailyPenalties(AnimalData animal, boolean isWinter) {
        SeasonManager.Season season = plugin.getSeasonManager().getCurrentSeason();
        boolean isGoodWeather = !isWinter && !isRainingOnWorld(animal);

        double penalty = 0;

        // Hunger
        if (animal.getLastFedDay() < currentServerDay - 1) {
            penalty -= plugin.getConfig().getDouble("farming.animals.affection.penalty_unfed", -2.0);
            animal.setHunger(Math.max(0, animal.getHunger() - 0.3));
        }

        // Outside in winter
        if (!animal.isInside() && isWinter) {
            penalty += plugin.getConfig().getDouble("farming.animals.affection.penalty_outside_winter", -1.5);
        }

        // Outside in storm
        if (!animal.isInside() && isRainingOnWorld(animal)) {
            penalty += plugin.getConfig().getDouble("farming.animals.affection.penalty_outside_storm", -1.0);
        }

        // Sick
        if (animal.isSick()) {
            penalty += plugin.getConfig().getDouble("farming.animals.affection.penalty_sick", -3.0);
        }

        // Cramped (inside all day in good weather)
        if (animal.isInside() && isGoodWeather && !isWinter) {
            penalty += plugin.getConfig().getDouble("farming.animals.affection.penalty_cramped", -0.5);
        }

        // Health decay from hunger
        if (animal.getHunger() < 0.3) {
            animal.setHealth(animal.getHealth() - 0.1);
        }

        // Health recovery if fed and healthy
        if (animal.getHunger() > 0.7 && !animal.isSick()) {
            animal.setHealth(Math.min(1.0, animal.getHealth() + 0.05));
        }

        animal.setAffection(animal.getAffection() + penalty);

        // Heal hunger if fed
        if (animal.getLastFedDay() == currentServerDay) {
            animal.setHunger(Math.min(1.0, animal.getHunger() + 0.2));
        }
    }

    // -- Sickness --

    private void checkSickness(AnimalData animal) {
        if (animal.isSick()) {
            // Check for recovery: fed + inside + affection > 2 for 3 days
            if (animal.getLastFedDay() >= currentServerDay - 1
                    && animal.isInside()
                    && animal.getAffection() > 2.0) {
                // 30% chance to recover each day
                if (random.nextDouble() < 0.3) {
                    animal.setSick(false);
                    animal.setHealth(Math.min(1.0, animal.getHealth() + 0.2));
                    plugin.getLogger().info("Animal " + animal.getName() + " recovered from sickness.");
                }
            }
            return;
        }

        // Check if should get sick
        boolean shouldGetSick = false;

        // 3+ days without food
        if (animal.getLastFedDay() < currentServerDay - 3) {
            shouldGetSick = true;
        }

        // Outside in bad weather
        if (!animal.isInside() && isRainingOnWorld(animal) && !hasShelterNearby(animal)) {
            shouldGetSick = random.nextDouble() < 0.5;
        }

        // Very low affection for 3+ days
        if (animal.getAffection() < 0.5 && animal.getLastInteractionDay() < currentServerDay - 3) {
            shouldGetSick = true;
        }

        // Base random chance
        double baseChance = plugin.getConfig().getDouble("farming.animals.sickness.base_daily_chance", 0.02);
        if (!shouldGetSick && random.nextDouble() < baseChance) {
            shouldGetSick = true;
        }

        if (shouldGetSick) {
            animal.setSick(true);
        }
    }

    // -- Death --

    private void checkDeath(AnimalData animal, Entity entity) {
        if (!animal.isSick()) return;

        int daysSickToDie = plugin.getConfig().getInt("farming.animals.sickness.days_sick_to_die", 7);
        // Rough check: if health reached 0 or if sick for too long (approximated)
        if (animal.getHealth() <= 0 || random.nextDouble() < 0.25) {
            // Animal dies
            entity.remove();
            unregisterAnimal(animal.getAnimalId());

            Player owner = Bukkit.getPlayer(animal.getOwnerId());
            if (owner != null && owner.isOnline()) {
                owner.sendMessage("§c\u2620 Your " + animal.getType().getDisplayName()
                        + " " + animal.getName() + " has died.");
                plugin.getPlayerJournalService().addEntry(owner,
                        "§c" + animal.getName() + " died.");
            }
        }
    }

    // -- Enclosure Detection --

    /** Checks if the location is inside an animal enclosure. */
    public boolean detectEnclosure(Location location) {
        int fenceCount = 0;
        int roofCount = 0;

        // Scan horizontally for fences (radius: 7x7)
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                Block block = location.getBlock().getRelative(dx, dz, 0);
                if (isFence(block.getType())) {
                    fenceCount++;
                }
            }
        }

        // Scan for a roof above (3 blocks up)
        for (int dy = 1; dy <= 3; dy++) {
            Block above = location.getBlock().getRelative(0, dy, 0);
            if (isRoof(above.getType())) {
                roofCount++;
            }
        }

        // Considered inside enclosure if: fences on ≥2 sides + any roof
        return fenceCount >= 2 || roofCount >= 1;
    }

    private boolean isFence(Material material) {
        for (Material fence : FENCE_BLOCKS) {
            if (fence == material) return true;
        }
        return false;
    }

    private boolean isRoof(Material material) {
        for (Material roof : ROOF_BLOCKS) {
            if (roof == material) return true;
        }
        return false;
    }

    private boolean hasShelterNearby(AnimalData animal) {
        Entity entity = findEntity(animal.getAnimalId());
        if (entity == null) return false;
        return detectEnclosure(entity.getLocation());
    }

    // -- Grazing Detection --

    /** Checks if the animal is standing on grass (grazing). */
    public boolean isGrazing(UUID animalId) {
        Entity entity = findEntity(animalId);
        if (entity == null) return false;

        Block below = entity.getLocation().getBlock().getRelative(0, -1, 0);
        for (Material grass : GRASS_BLOCKS) {
            if (below.getType() == grass) return true;
        }
        return false;
    }

    /** Checks for a HAY_BLOCK feeder nearby. */
    public boolean hasHayNearby(UUID animalId) {
        Entity entity = findEntity(animalId);
        if (entity == null) return false;

        Location loc = entity.getLocation();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    Block block = loc.getBlock().getRelative(dx, dy, dz);
                    if (block.getType() == Material.HAY_BLOCK) {
                        // Consume one hay block (degrade it)
                        // In a real impl, we might track hay consumption
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // -- Product Quality --

    /** Determines product quality based on affection. */
    public ProductQuality determineProductQuality(AnimalData animal) {
        double affection = animal.getAffection();
        double roll = random.nextDouble();

        if (affection >= 9.1) return ProductQuality.GOLD_PLUS;  // 100% Gold+
        if (affection >= 7.1) {
            if (roll < 0.9) return ProductQuality.GOLD;
            return ProductQuality.GOLD_PLUS;
        }
        if (affection >= 5.1) {
            if (roll < 0.2) return ProductQuality.SILVER;
            if (roll < 0.9) return ProductQuality.GOLD;
            return ProductQuality.GOLD_PLUS;
        }
        if (affection >= 3.1) {
            if (roll < 0.4) return ProductQuality.BRONZE;
            if (roll < 0.9) return ProductQuality.SILVER;
            return ProductQuality.GOLD;
        }
        if (affection >= 1.1) {
            if (roll < 0.7) return ProductQuality.BRONZE;
            return ProductQuality.SILVER;
        }
        return ProductQuality.BRONZE;
    }

    public enum ProductQuality {
        BRONZE("§6Bronze", 1),
        SILVER("§7Silver", 2),
        GOLD("§eGold", 3),
        GOLD_PLUS("§bGold+", 4);

        private final String displayName;
        private final int tier;

        ProductQuality(String displayName, int tier) {
            this.displayName = displayName;
            this.tier = tier;
        }

        public String getDisplayName() { return displayName; }
        public int getTier() { return tier; }
        public double getBonusMultiplier() { return 0.5 + tier * 0.5; }
    }

    // -- Utility --

    private Entity findEntity(UUID uuid) {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(uuid)) {
                    return entity;
                }
            }
        }
        return null;
    }

    private boolean isRainingOnWorld(AnimalData animal) {
        Entity entity = findEntity(animal.getAnimalId());
        if (entity == null) return false;
        World world = entity.getWorld();
        return world.hasStorm() || world.isThundering();
    }

    public int getCurrentServerDay() {
        return currentServerDay;
    }
}
