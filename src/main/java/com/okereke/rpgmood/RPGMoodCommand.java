package com.okereke.rpgmood;

import com.okereke.rpgmood.menu.MainMenu;
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

        if (args[0].equalsIgnoreCase("zones")) {
            return handleZones(sender, args);
        }

        if (args[0].equalsIgnoreCase("level")) {
            return handleLevel(sender);
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
                case "scoreboard" -> {
                    String configKey = "player_scoreboard.";
                    boolean enabled = !plugin.getConfigManager().getConfigValues().getBoolean(configKey + player.getUniqueId(), true);
                    plugin.getConfigManager().savePlayerToggle(configKey, player.getUniqueId(), enabled);
                    if (enabled) {
                        plugin.getZoneScoreboardService().updateScoreboard(player);
                    } else {
                        plugin.getZoneScoreboardService().clearScoreboard(player);
                    }
                    player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                            enabled ? "&a[RPGMood] Sidebar scoreboard enabled." : "&e[RPGMood] Sidebar scoreboard disabled."));
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

        sender.sendMessage(Component.text("Usage: /rpgmood menu|reload|toggle [titles|scoreboard]|info|leaderboard [deaths|zones|level]|achievements|zones [fav <name>]|level").color(NamedTextColor.RED));
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

    /** Lists zones the player has discovered, or marks one as a favorite with "zones fav <name>". */
    private boolean handleZones(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("rpgmood.player.zones")) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfig().getString("messages.no_permission", "&cNo permission")));
            return true;
        }

        ZoneDiscoveryService discovery = plugin.getZoneDiscoveryService();

        if (args.length > 1 && args[1].equalsIgnoreCase("fav")) {
            if (args.length < 3) {
                player.sendMessage(Component.text("Usage: /rpgmood zones fav <name>").color(NamedTextColor.RED));
                return true;
            }
            String name = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
            Boolean nowFavorite = discovery.toggleFavorite(player, name);
            if (nowFavorite == null) {
                player.sendMessage(Component.text("You haven't discovered a zone called \"" + name + "\".").color(NamedTextColor.RED));
            } else {
                player.sendMessage(Component.text(nowFavorite ? "★ Marked as favorite: " : "☆ Unmarked: ", NamedTextColor.YELLOW)
                        .append(Component.text(name, NamedTextColor.WHITE)));
            }
            return true;
        }

        List<ZoneDiscoveryService.DiscoveredZone> zones = discovery.getDiscoveries(player);
        if (zones.isEmpty()) {
            player.sendMessage(Component.text("You haven't discovered any zones yet — go explore!").color(NamedTextColor.GRAY));
            return true;
        }

        player.sendMessage(Component.text("=== Discovered Zones (" + zones.size() + ") ===").color(NamedTextColor.GOLD));
        var dateFormat = new java.text.SimpleDateFormat("yyyy-MM-dd");
        for (ZoneDiscoveryService.DiscoveredZone zone : zones) {
            String date = dateFormat.format(new java.util.Date(zone.firstSeenMillis()));
            String biome = discovery.biomeOf(zone.key());
            Integer danger = discovery.dangerLevelOf(player, zone.key());
            player.sendMessage(Component.text()
                    .append(Component.text(zone.favorite() ? "★ " : "  ", NamedTextColor.YELLOW))
                    .append(Component.text(zone.display(), NamedTextColor.WHITE))
                    .append(Component.text(" (" + biome + (danger != null ? ", Lv " + danger : "") + ")", NamedTextColor.GRAY))
                    .append(Component.text(" — first seen " + date, NamedTextColor.DARK_GRAY)));
        }

        Double nearest = discovery.distanceToNearestDiscoveredZone(player);
        if (nearest != null) {
            player.sendMessage(Component.text("Nearest known zone: ", NamedTextColor.GRAY)
                    .append(Component.text((int) (double) nearest + " blocks away", NamedTextColor.YELLOW)));
        }
        player.sendMessage(Component.text("Tip: /rpgmood zones fav <name> to mark a favorite.", NamedTextColor.DARK_GRAY));
        return true;
    }

    /** Shows the player's RPG progression level/XP — a separate track from mob difficulty levels. */
    private boolean handleLevel(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("rpgmood.player.level")) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfig().getString("messages.no_permission", "&cNo permission")));
            return true;
        }

        PlayerLevelService levels = plugin.getPlayerLevelService();
        int level = levels.getLevel(player);
        long xp = levels.getXp(player);
        long xpForCurrent = PlayerLevelService.xpForLevel(level);
        long xpForNext = PlayerLevelService.xpForLevel(level + 1);
        long into = xp - xpForCurrent;
        long needed = xpForNext - xpForCurrent;
        int animalCapBonus = levels.getAnimalCapBonus(player);

        player.sendMessage(Component.text("=== RPGMood Level ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("Level: ").color(NamedTextColor.GRAY).append(Component.text(level, NamedTextColor.WHITE)));
        player.sendMessage(Component.text("XP: ").color(NamedTextColor.GRAY)
                .append(Component.text(into + " / " + needed, NamedTextColor.YELLOW))
                .append(Component.text(" to next level", NamedTextColor.GRAY)));
        if (animalCapBonus > 0) {
            player.sendMessage(Component.text("Animal cap bonus: ").color(NamedTextColor.GRAY)
                    .append(Component.text("+" + animalCapBonus, NamedTextColor.GREEN)));
        }
        player.sendMessage(Component.text("Earn XP by killing scaled mobs and discovering new zones.", NamedTextColor.DARK_GRAY));
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
            return filterStartsWith(List.of("menu", "reload", "toggle", "info", "leaderboard", "achievements", "zones", "level"), args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("toggle")) {
            return filterStartsWith(List.of("titles", "scoreboard"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("leaderboard")) {
            return filterStartsWith(List.of("deaths", "zones", "level"), args[1]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("zones")) {
            return filterStartsWith(List.of("fav"), args[1]);
        }
        return List.of();
    }

    private static List<String> filterStartsWith(List<String> options, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.startsWith(lower)).toList();
    }
}
