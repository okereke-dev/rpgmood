package com.ricardo.rpgmood.farming;

import com.ricardo.rpgmood.RPGMoodPlugin;
import com.ricardo.rpgmood.farming.animal.AnimalData;
import com.ricardo.rpgmood.farming.animal.AnimalManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Handles /rpgmood-farm subcommands: season, crops, recipes, animal.
 */
public class FarmingCommand implements CommandExecutor {

    private final RPGMoodPlugin plugin;

    public FarmingCommand(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "season" -> handleSeason(player);
            case "crops" -> handleCrops(player);
            case "recipes" -> handleRecipes(player, args);
            case "animal" -> handleAnimal(player, args);
            default -> showHelp(player);
        }
        return true;
    }

    private void showHelp(Player player) {
        player.sendMessage(Component.text("=== RPGMood Farming ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("/rpgmood-farm season").color(NamedTextColor.GRAY)
                .append(Component.text(" — Show current season").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/rpgmood-farm crops").color(NamedTextColor.GRAY)
                .append(Component.text(" — List seasonal crops").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/rpgmood-farm recipes").color(NamedTextColor.GRAY)
                .append(Component.text(" — Show recipes").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/rpgmood-farm animal list").color(NamedTextColor.GRAY)
                .append(Component.text(" — List your animals").color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("/rpgmood-farm animal info").color(NamedTextColor.GRAY)
                .append(Component.text(" — Look at animal with stick").color(NamedTextColor.WHITE)));
    }

    // -- Season --

    private void handleSeason(Player player) {
        SeasonManager sm = plugin.getSeasonManager();
        SeasonManager.Season current = sm.getCurrentSeason();
        int day = sm.getDayInSeason();
        int seasonLength = plugin.getConfig().getInt("farming.season_length_days", 30);
        int daysLeft = seasonLength - day;
        double growthMult = sm.getGrowthMultiplier();

        player.sendMessage(Component.text("=== Current Season ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text()
                .append(Component.text(current.getIcon() + " ", NamedTextColor.WHITE))
                .append(Component.text(current.getDisplayName(), NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("Day ").color(NamedTextColor.GRAY)
                .append(Component.text(day + 1, NamedTextColor.WHITE))
                .append(Component.text(" of ", NamedTextColor.GRAY))
                .append(Component.text(seasonLength, NamedTextColor.WHITE))
                .append(Component.text(" (" + daysLeft + " days remaining)", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("Growth multiplier: ").color(NamedTextColor.GRAY)
                .append(Component.text("x" + String.format("%.1f", growthMult), NamedTextColor.GREEN)));
        player.sendMessage(Component.text(current.getDescription()).color(NamedTextColor.DARK_GRAY));
    }

    // -- Crops --

    private void handleCrops(Player player) {
        SeasonManager sm = plugin.getSeasonManager();
        List<String> seasonalCrops = sm.getSeasonalCrops();

        player.sendMessage(Component.text("=== Seasonal Crops (" + sm.getCurrentSeason().getDisplayName() + ") ===")
                .color(NamedTextColor.GOLD));

        if (seasonalCrops.isEmpty()) {
            player.sendMessage(Component.text("No crops this season. Try foraging!").color(NamedTextColor.GRAY));
            return;
        }

        for (String cropKey : seasonalCrops) {
            CropManager.CropDefinition def = plugin.getCropManager().getCropDefinitions().get(cropKey);
            if (def != null) {
                player.sendMessage(Component.text()
                        .append(Component.text(" • ", NamedTextColor.GRAY))
                        .append(Component.text(cropKey.substring(0, 1).toUpperCase() + cropKey.substring(1), NamedTextColor.WHITE))
                        .append(Component.text(" (" + def.material().name() + ")", NamedTextColor.DARK_GRAY)));
            } else {
                player.sendMessage(Component.text(" • " + cropKey, NamedTextColor.WHITE));
            }
        }
    }

    // -- Recipes --

    private void handleRecipes(Player player, String[] args) {
        boolean showAll = args.length > 1 && args[1].equalsIgnoreCase("all");
        RecipeManager rm = plugin.getRecipeManager();

        if (showAll) {
            player.sendMessage(Component.text("=== All Recipes ===").color(NamedTextColor.GOLD));
            List<Recipe> all = rm.getAllRecipes();
            if (all.isEmpty()) {
                player.sendMessage(Component.text("No recipes configured.").color(NamedTextColor.GRAY));
                return;
            }
            for (Recipe recipe : all) {
                boolean discovered = rm.hasDiscovered(player, recipe.id());
                player.sendMessage(Component.text()
                        .append(Component.text(discovered ? "✅ " : "⬛ ", NamedTextColor.GRAY))
                        .append(LegacyComponentSerializer.legacyAmpersand().deserialize(recipe.name()))
                        .append(Component.text(" [" + recipe.effect() + "]", NamedTextColor.DARK_GRAY)));
            }
        } else {
            player.sendMessage(Component.text("=== Your Recipes (" + rm.getDiscoveredCount(player) + "/" + rm.getAllRecipes().size() + ") ===")
                    .color(NamedTextColor.GOLD));
            List<String> discoveredIds = rm.getDiscoveredRecipes(player);
            if (discoveredIds.isEmpty()) {
                player.sendMessage(Component.text("No recipes discovered yet. Experiment in a crafting table!").color(NamedTextColor.GRAY));
                return;
            }
            for (String id : discoveredIds) {
                Recipe recipe = rm.getRecipe(id);
                if (recipe != null) {
                    player.sendMessage(Component.text()
                            .append(Component.text(" ✅ ", NamedTextColor.GREEN))
                            .append(LegacyComponentSerializer.legacyAmpersand().deserialize(recipe.name()))
                            .append(Component.text(" → ", NamedTextColor.DARK_GRAY))
                            .append(Component.text(recipe.description(), NamedTextColor.GRAY)));
                }
            }
            player.sendMessage(Component.text("\nUse /rpgmood-farm recipes all to see all recipes.")
                    .color(NamedTextColor.DARK_GRAY));
        }
    }

    // -- Animal Commands --

    private void handleAnimal(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /rpgmood-farm animal list|info").color(NamedTextColor.RED));
            return;
        }

        switch (args[1].toLowerCase()) {
            case "list" -> handleAnimalList(player);
            case "info" -> handleAnimalInfo(player);
            default -> player.sendMessage(Component.text("Unknown subcommand. Use: list, info").color(NamedTextColor.RED));
        }
    }

    private void handleAnimalList(Player player) {
        AnimalManager manager = plugin.getAnimalManager();
        List<AnimalData> animals = manager.getOwnedAnimals(player.getUniqueId());

        if (animals.isEmpty()) {
            player.sendMessage(Component.text("You don't own any animals yet.").color(NamedTextColor.GRAY));
            player.sendMessage(Component.text("Find a wild cow/chicken/sheep/goat and feed it its favorite food to befriend it!").color(NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("=== Your Animals (" + animals.size() + ") ===").color(NamedTextColor.GOLD));
        for (AnimalData animal : animals) {
            int hearts = animal.getHeartLevel();
            String heartDisplay = "❤️".repeat(hearts) + "♡".repeat(5 - hearts);
            player.sendMessage(Component.text()
                    .append(Component.text(" • ", NamedTextColor.GRAY))
                    .append(Component.text(animal.getName(), NamedTextColor.WHITE))
                    .append(Component.text(" the " + animal.getType().getDisplayName(), NamedTextColor.GRAY))
                    .append(Component.text(" " + heartDisplay, NamedTextColor.RED))
                    .append(Component.text(animal.isSick() ? " ☠ Sick" : " ✔ Healthy",
                            animal.isSick() ? NamedTextColor.RED : NamedTextColor.GREEN))
                    .build());
        }
    }

    private void handleAnimalInfo(Player player) {
        player.sendMessage(Component.text("Look at an animal and right-click with a stick to see its info!")
                .color(NamedTextColor.YELLOW));
    }
}
