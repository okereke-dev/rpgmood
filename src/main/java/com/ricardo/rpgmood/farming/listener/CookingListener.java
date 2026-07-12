package com.ricardo.rpgmood.farming.listener;

import com.ricardo.rpgmood.RPGMoodPlugin;
import com.ricardo.rpgmood.farming.CropQuality;
import com.ricardo.rpgmood.farming.Recipe;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Handles cooking: discovering recipes via crafting grid experimentation
 * and detecting campfire/furnace cooking with RPGMood ingredients.
 */
public class CookingListener implements Listener {

    private final RPGMoodPlugin plugin;

    public CookingListener(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Detect when a player places ingredients in a crafting grid to discover a recipe.
     * If the combination matches a known recipe, mark it as discovered.
     */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (!(event.getView().getPlayer() instanceof Player player)) return;
        CraftingInventory inventory = event.getInventory();
        if (inventory.getSize() < 4) return; // not a crafting table

        // Get the ingredients from the crafting grid
        List<Material> ingredients = new ArrayList<>();
        for (ItemStack item : inventory.getMatrix()) {
            if (item != null && item.getType() != Material.AIR) {
                ingredients.add(item.getType());
            }
        }

        if (ingredients.isEmpty()) return;

        Recipe recipe = plugin.getRecipeManager().findRecipe(ingredients);
        if (recipe == null) return;

        // Check if player has discovered this recipe
        boolean discovered = plugin.getRecipeManager().hasDiscovered(player, recipe.id());
        if (!discovered) {
            // New discovery!
            plugin.getRecipeManager().discoverRecipe(player, recipe.id());
            plugin.getAchievementManager().onRecipeDiscovered(player,
                    plugin.getRecipeManager().getDiscoveredCount(player),
                    plugin.getRecipeManager().getAllRecipes().size());
            player.sendMessage(Component.text()
                    .append(Component.text("\u2728 New recipe discovered! ", NamedTextColor.GOLD))
                    .append(LegacyComponentSerializer.legacyAmpersand().deserialize(recipe.name()))
                    .build());

            plugin.getPlayerJournalService().addEntry(player,
                    "Discovered recipe: " + recipe.name() + "!");
        }

        // Set the result
        ItemStack result = new ItemStack(recipe.result());
        result.editMeta(meta -> {
            meta.setDisplayName(org.bukkit.ChatColor.translateAlternateColorCodes('&', recipe.name()));
            var lore = new ArrayList<String>();
            lore.add("§7" + recipe.description());
            lore.add("§eEffect: " + recipe.effect() + " (" + recipe.effectDuration() + "s)");
            lore.add("§fRequires: " + recipe.minQuality().getDisplayName() + " ingredients");
            meta.setLore(lore);
        });
        event.getInventory().setResult(result);
    }

    /**
     * Apply mood effects when a player crafts a known recipe.
     */
    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack result = event.getCurrentItem();
        if (result == null || !result.hasItemMeta()) return;

        String displayName = result.getItemMeta().getDisplayName();
        if (displayName == null || displayName.isEmpty()) return;

        // Find which recipe matches this result
        for (Recipe recipe : plugin.getRecipeManager().getAllRecipes()) {
            String recipeName = org.bukkit.ChatColor.stripColor(
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', recipe.name()));
            String resultName = org.bukkit.ChatColor.stripColor(displayName);

            if (resultName.equals(recipeName) && recipe.result() == result.getType()) {
                // Apply mood effect
                applyMoodEffect(player, recipe);
                plugin.getPlayerJournalService().addEntry(player,
                        "Cooked: " + recipe.name() + " (" + recipe.effect() + ")");
                return;
            }
        }
    }

    /**
     * Applies the configured potion effect from the recipe's mood_effects config.
     */
    private void applyMoodEffect(Player player, Recipe recipe) {
        var effectsSection = plugin.getConfig().getConfigurationSection("farming.mood_effects");
        if (effectsSection == null) return;

        String effectPath = "farming.mood_effects." + recipe.effect();
        String effectName = plugin.getConfig().getString(effectPath + ".effect", "");
        int amplifier = plugin.getConfig().getInt(effectPath + ".amplifier", 0);

        if (effectName.isEmpty()) return;

        try {
            org.bukkit.potion.PotionEffectType effectType =
                    org.bukkit.potion.PotionEffectType.getByName(effectName);
            if (effectType != null) {
                int duration = recipe.effectDuration() * 20; // convert seconds to ticks
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        effectType, duration, amplifier, true, true));

                String description = effectsSection.getString(recipe.effect() + ".description",
                        "&aMood effect applied!");
                player.sendMessage(LegacyComponentSerializer.legacyAmpersand()
                        .deserialize(description));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply mood effect '" + effectName + "': " + e.getMessage());
        }
    }

    /**
     * Right-click a campfire with recipe ingredients to cook (alternative discovery method).
     */
    @EventHandler
    public void onCampfireInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Material type = block.getType();
        boolean isCampfire = type == Material.CAMPFIRE || type == Material.SOUL_CAMPFIRE;
        if (!isCampfire) return;

        Player player = event.getPlayer();
        ItemStack hand = event.getItem();
        if (hand == null || hand.getType() == Material.AIR) return;

        // Let vanilla campfire cooking happen normally
        // RPGMood recipes are handled through the crafting grid system
        player.sendActionBar(Component.text("Try combining ingredients in a crafting table to discover recipes!")
                .color(NamedTextColor.GRAY));
    }
}
