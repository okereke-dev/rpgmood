package com.ricardo.rpgmood;

import com.ricardo.rpgmood.menu.MainMenu;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

public class RPGMoodCommand implements CommandExecutor, TabCompleter {

    private final RPGMoodPlugin plugin;

    public RPGMoodCommand(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            return handleMenu(sender);
        }

        if (args[0].equalsIgnoreCase("info")) {
            return handleInfo(sender);
        }

        if (args[0].equalsIgnoreCase("leaderboard")) {
            return handleLeaderboard(sender, args);
        }

        if (args[0].equalsIgnoreCase("achievements")) {
            return handleAchievements(sender, args);
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("rpgmood.admin")) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfig().getString("messages.no_permission", "&cNo permission")));
                return true;
            }
            plugin.getConfigManager().reload();
            plugin.getMobScalingService().invalidateStructureCache();
            // Reload farming data
            if (plugin.getCropManager() != null) {
                plugin.getCropManager().reload();
            }
            if (plugin.getRecipeManager() != null) {
                plugin.getRecipeManager().reload();
            }
            if (plugin.getAnimalManager() != null) {
                plugin.getAnimalManager().reload();
            }
            if (plugin.getAchievementManager() != null) {
                plugin.getAchievementManager().reload();
            }
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfig().getString("messages.plugin_reloaded", "&aReloaded")));
            return true;
        }

        if (args[0].equalsIgnoreCase("toggle")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
                return true;
            }
            if (!player.hasPermission("rpgmood.player.toggle")) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfig().getString("messages.no_permission", "&cNo permission")));
                return true;
            }

            String sub = args.length > 1 ? args[1].toLowerCase() : "";

            switch (sub) {
                case "titles" -> {
                    String configKey = "player_titles.";
                    boolean enabled = !plugin.getConfigManager().getConfigValues().getBoolean(configKey + player.getUniqueId(), true);
                    plugin.getConfigManager().savePlayerToggle(configKey, player.getUniqueId(), enabled);
                    player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                            enabled ? "&a[RPGMood] Zone titles enabled." : "&e[RPGMood] Zone titles disabled."));
                }
                default -> {
                    // Master toggle: zone feedback + ambient messages
                    String configKey = "player_effects.";
                    boolean enabled = !plugin.getConfigManager().getConfigValues().getBoolean(configKey + player.getUniqueId(), true);
                    plugin.getConfigManager().savePlayerToggle(configKey, player.getUniqueId(), enabled);
                    player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                            enabled ? plugin.getConfig().getString("messages.toggle_enabled", "&aEffects enabled")
                                    : plugin.getConfig().getString("messages.toggle_disabled", "&eEffects disabled")));
                }
            }
            return true;
        }

        sender.sendMessage(Component.text("Usage: /rpgmood menu|reload|toggle [titles]|info|leaderboard [deaths|zones|level]|achievements").color(NamedTextColor.RED));
        return true;
    }

    private boolean handleMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }
        MainMenu menu = new MainMenu(plugin, player);
        player.openInventory(menu.getInventory());
        return true;
    }

    private boolean handleLeaderboard(CommandSender sender, String[] args) {
        if (!sender.hasPermission("rpgmood.player.leaderboard")) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfig().getString("messages.no_permission", "&cNo permission")));
            return true;
        }

        String category = args.length > 1 ? args[1].toLowerCase() : "deaths";
        java.util.List<PlayerStatsService.StatEntry> top;
        String title;
        switch (category) {
            case "zones" -> {
                top = plugin.getPlayerStatsService().getTopZoneChanges(10);
                title = "Zone Changes";
            }
            case "level" -> {
                top = plugin.getPlayerStatsService().getTopMobLevel(10);
                title = "Highest Mob Level Killed";
            }
            default -> {
                top = plugin.getPlayerStatsService().getTopDeaths(10);
                title = "Deaths";
            }
        }

        sender.sendMessage(Component.text("=== " + title + " Leaderboard ===").color(NamedTextColor.GOLD));
        if (top.isEmpty()) {
            sender.sendMessage(Component.text("No data yet.").color(NamedTextColor.GRAY));
            return true;
        }
        int rank = 1;
        for (PlayerStatsService.StatEntry entry : top) {
            sender.sendMessage(Component.text(rank + ". ").color(NamedTextColor.GRAY)
                    .append(Component.text(entry.name()).color(NamedTextColor.WHITE))
                    .append(Component.text(" — " + entry.value()).color(NamedTextColor.YELLOW)));
            rank++;
        }
        return true;
    }

    private boolean handleAchievements(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("rpgmood.player.achievements")) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfig().getString("messages.no_permission", "&cNo permission")));
            return true;
        }

        AchievementManager achievements = plugin.getAchievementManager();
        int unlockedCount = achievements.getUnlockedCount(player);
        int total = AchievementManager.ALL_ACHIEVEMENTS.size();

        player.sendMessage(Component.text("=== Achievements (" + unlockedCount + "/" + total + ") ===").color(NamedTextColor.GOLD));
        for (AchievementManager.Category category : AchievementManager.Category.values()) {
            List<AchievementManager.Achievement> inCategory = AchievementManager.ALL_ACHIEVEMENTS.stream()
                    .filter(a -> a.category() == category)
                    .toList();
            if (inCategory.isEmpty()) continue;

            int categoryUnlocked = (int) inCategory.stream().filter(a -> achievements.hasUnlocked(player, a.id())).count();
            player.sendMessage(Component.text("-- " + capitalize(category.name()) + " (" + categoryUnlocked + "/" + inCategory.size() + ") --")
                    .color(NamedTextColor.YELLOW));

            for (AchievementManager.Achievement ach : inCategory) {
                boolean unlocked = achievements.hasUnlocked(player, ach.id());
                if (unlocked) {
                    player.sendMessage(Component.text()
                            .append(Component.text("✅ " + ach.icon() + " ", NamedTextColor.GREEN))
                            .append(Component.text(ach.name(), NamedTextColor.WHITE))
                            .append(Component.text(" — " + ach.description(), NamedTextColor.GRAY)));
                } else {
                    player.sendMessage(Component.text()
                            .append(Component.text("⬛ " + ach.icon() + " ", NamedTextColor.DARK_GRAY))
                            .append(Component.text(ach.name(), NamedTextColor.DARK_GRAY)));
                }
            }
        }
        return true;
    }

    private static String capitalize(String enumName) {
        String lower = enumName.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    /** Admin/debug view of the zone and mob difficulty at the sender's current location. */
    private boolean handleInfo(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("rpgmood.admin")) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfig().getString("messages.no_permission", "&cNo permission")));
            return true;
        }

        String zone = plugin.getZoneManager().getCurrentZoneDisplayName(player);
        String biome = player.getLocation().getBlock().getBiome().name();
        double distanceFromSpawn = player.getLocation().distance(player.getWorld().getSpawnLocation());

        player.sendMessage(Component.text("=== RPGMood Info ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("Zone: ").color(NamedTextColor.GRAY).append(Component.text(zone).color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Biome: ").color(NamedTextColor.GRAY).append(Component.text(biome).color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Distance from spawn: ").color(NamedTextColor.GRAY).append(Component.text((int) distanceFromSpawn + " blocks").color(NamedTextColor.WHITE)));

        for (EntityType type : new EntityType[]{EntityType.ZOMBIE, EntityType.SKELETON, EntityType.ENDERMAN}) {
            int baseLevel = plugin.getMobScalingService().getBaseLevel(type);
            int level = plugin.getMobScalingService().calculateLevelAt(player.getLocation(), baseLevel);
            player.sendMessage(Component.text(type.name() + " would spawn at level: ").color(NamedTextColor.GRAY).append(Component.text(level).color(NamedTextColor.YELLOW)));
        }
        return true;
    }

    // -- Tab completion --

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(List.of("menu", "reload", "toggle", "info", "leaderboard", "achievements"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
            return filterStartsWith(List.of("titles"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("leaderboard")) {
            return filterStartsWith(List.of("deaths", "zones", "level"), args[1]);
        }
        return List.of();
    }

    private static List<String> filterStartsWith(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.startsWith(lower)).toList();
    }
}
