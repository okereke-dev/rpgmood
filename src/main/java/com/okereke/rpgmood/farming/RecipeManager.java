package com.okereke.rpgmood.farming;

import com.okereke.rpgmood.RPGMoodPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages cooking recipe discovery and validation.
 * Recipes are defined in farming.yml and can be "discovered" by players
 * through experimentation or learning from books. Discoveries are persisted
 * to recipes.yml, saved on every new discovery (infrequent, unlike combat/farming
 * progress which uses the debounced-save pattern in AchievementManager).
 */
public class RecipeManager {

    private final RPGMoodPlugin plugin;
    private final File dataFile;
    private final List<Recipe> allRecipes = new ArrayList<>();
    private final Map<UUID, List<String>> discoveredRecipes = new HashMap<>(); // player UUID -> recipe IDs

    public RecipeManager(RPGMoodPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "recipes.yml");
        loadRecipes();
        loadDiscoveries();
    }

    /** Loads all recipes from farming.yml. */
    private void loadRecipes() {
        allRecipes.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("farming.recipes");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String path = "farming.recipes." + key;
            String name = section.getString(key + ".name", key);
            List<String> rawIngredients = section.getStringList(key + ".ingredients");
            List<Material> ingredients = new ArrayList<>();
            for (String mat : rawIngredients) {
                Material m = Material.getMaterial(mat.toUpperCase());
                if (m != null) ingredients.add(m);
            }
            String resultStr = section.getString(key + ".result", "MUSHROOM_STEW");
            Material result = Material.getMaterial(resultStr.toUpperCase());
            String effect = section.getString(key + ".effect", "fortified");
            int effectDuration = section.getInt(key + ".effect_duration", 60);
            String minQualityStr = section.getString(key + ".min_quality", "BRONZE");
            CropQuality minQuality = CropQuality.valueOf(minQualityStr.toUpperCase());
            String description = section.getString(key + ".description", "");

            if (result != null && !ingredients.isEmpty()) {
                allRecipes.add(new Recipe(
                        key, name, Collections.unmodifiableList(ingredients),
                        result, effect, effectDuration, minQuality, description
                ));
            }
        }
    }

    public void reload() {
        loadRecipes();
        loadDiscoveries();
    }

    /** Loads discovered recipes for all players from recipes.yml into the in-memory map. */
    private void loadDiscoveries() {
        discoveredRecipes.clear();
        YamlConfiguration data = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection players = data.getConfigurationSection("players");
        if (players == null) return;
        for (String uuid : players.getKeys(false)) {
            discoveredRecipes.put(UUID.fromString(uuid), new ArrayList<>(players.getStringList(uuid)));
        }
    }

    /** Persists all players' discovered recipes to recipes.yml. */
    private void saveDiscoveries() {
        YamlConfiguration data = new YamlConfiguration();
        for (Map.Entry<UUID, List<String>> entry : discoveredRecipes.entrySet()) {
            data.set("players." + entry.getKey(), entry.getValue());
        }
        try {
            data.save(dataFile);
        } catch (java.io.IOException e) {
            plugin.getLogger().warning("Could not save recipes.yml: " + e.getMessage());
        }
    }

    /** Returns an unmodifiable list of all available recipes. */
    public List<Recipe> getAllRecipes() {
        return List.copyOf(allRecipes);
    }

    /** Attempts to find a recipe matching the given ingredients. */
    public Recipe findRecipe(List<Material> ingredients) {
        for (Recipe recipe : allRecipes) {
            if (recipe.matches(ingredients)) {
                return recipe;
            }
        }
        return null;
    }

    /** Finds a recipe by its ID. */
    public Recipe getRecipe(String id) {
        for (Recipe recipe : allRecipes) {
            if (recipe.id().equals(id)) {
                return recipe;
            }
        }
        return null;
    }

    // -- Discovery system

    /** Returns the list of recipe IDs a player has discovered. */
    public List<String> getDiscoveredRecipes(Player player) {
        return discoveredRecipes.getOrDefault(player.getUniqueId(), List.of());
    }

    /** Returns true if the player has discovered the given recipe. */
    public boolean hasDiscovered(Player player, String recipeId) {
        return getDiscoveredRecipes(player).contains(recipeId);
    }

    /** Marks a recipe as discovered by the player. Returns true if newly discovered. */
    public boolean discoverRecipe(Player player, String recipeId) {
        UUID id = player.getUniqueId();
        List<String> playerRecipes = discoveredRecipes.computeIfAbsent(id, k -> new ArrayList<>());
        if (playerRecipes.contains(recipeId)) {
            return false;
        }
        playerRecipes.add(recipeId);
        saveDiscoveries();
        return true;
    }

    /** Returns the count of discovered recipes for a player. */
    public int getDiscoveredCount(Player player) {
        return getDiscoveredRecipes(player).size();
    }

    /** Resets recipe discovery for a player (admin command). */
    public void resetDiscoveries(Player player) {
        discoveredRecipes.remove(player.getUniqueId());
        saveDiscoveries();
    }
}
