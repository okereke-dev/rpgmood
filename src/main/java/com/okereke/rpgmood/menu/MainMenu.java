package com.okereke.rpgmood.menu;

import com.okereke.rpgmood.AchievementManager;
import com.okereke.rpgmood.RPGMoodPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

/**
 * Entry point for all RPGMood info — opened via {@code /rpgmood} (no args) or
 * {@code /rpgmood menu}. Grows one icon per feature area; see each icon's own menu class.
 */
public class MainMenu implements RPGMoodMenu {

    private static final int SLOT_JOURNAL = 10;
    private static final int SLOT_ACHIEVEMENTS = 11;
    private static final int SLOT_ZONE = 12;
    private static final int SLOT_LEADERBOARD = 13;
    private static final int SLOT_FARMING = 14;
    private static final int SLOT_ANIMALS = 15;
    private static final int SLOT_RPGLOOT = 16;
    private static final int SLOT_LEVEL = 20;
    private static final int SLOT_SETTINGS = 22;
    private static final int SLOT_ADMIN_CONFIG = 24;

    private final RPGMoodPlugin plugin;
    private final Inventory inventory;

    public MainMenu(RPGMoodPlugin plugin, Player viewer) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("✦ RPGMood", NamedTextColor.GOLD));
        render(viewer);
    }

    @Override
    public Inventory getInventory() { return inventory; }

    private void render(Player viewer) {
        MenuUtil.fillBorder(inventory);

        inventory.setItem(SLOT_JOURNAL, MenuUtil.icon(Material.WRITTEN_BOOK,
                Component.text("📖 Adventure Journal", NamedTextColor.YELLOW),
                List.of(Component.text("Your recent zone arrivals and deaths.", NamedTextColor.GRAY))));

        int unlocked = plugin.getAchievementManager().getUnlockedCount(viewer);
        int total = AchievementManager.ALL_ACHIEVEMENTS.size();
        inventory.setItem(SLOT_ACHIEVEMENTS, MenuUtil.icon(Material.NETHER_STAR,
                Component.text("🌟 Achievements", NamedTextColor.YELLOW),
                List.of(Component.text(unlocked + "/" + total + " unlocked", NamedTextColor.GRAY))));

        inventory.setItem(SLOT_ZONE, MenuUtil.icon(Material.COMPASS,
                Component.text("🧭 Current Zone", NamedTextColor.YELLOW),
                List.of(Component.text("Zone, biome, and local mob danger.", NamedTextColor.GRAY))));

        inventory.setItem(SLOT_LEADERBOARD, MenuUtil.icon(Material.DIAMOND_SWORD,
                Component.text("🏅 Leaderboard", NamedTextColor.YELLOW),
                List.of(Component.text("Top 10 by deaths, zones, or mob level.", NamedTextColor.GRAY))));

        inventory.setItem(SLOT_FARMING, MenuUtil.icon(Material.WHEAT,
                Component.text("🌾 Farming", NamedTextColor.YELLOW),
                List.of(Component.text("Current season and its crops.", NamedTextColor.GRAY))));

        inventory.setItem(SLOT_ANIMALS, MenuUtil.icon(Material.HAY_BLOCK,
                Component.text("🐄 My Animals", NamedTextColor.YELLOW),
                List.of(Component.text("Your befriended cows, chickens,", NamedTextColor.GRAY),
                        Component.text("sheep, and goats.", NamedTextColor.GRAY))));

        if (Bukkit.getPluginManager().getPlugin("RPGLoot") != null) {
            inventory.setItem(SLOT_RPGLOOT, MenuUtil.icon(Material.NETHERITE_SWORD,
                    Component.text("⚔ RPGLoot", NamedTextColor.YELLOW),
                    List.of(Component.text("Equipped rarities, active set,", NamedTextColor.GRAY),
                            Component.text("and lifetime loot stats.", NamedTextColor.GRAY))));
        }

        int level = plugin.getPlayerLevelService().getLevel(viewer);
        long xp = plugin.getPlayerLevelService().getXp(viewer);
        long xpForCurrent = com.okereke.rpgmood.PlayerLevelService.xpForLevel(level);
        long xpForNext = com.okereke.rpgmood.PlayerLevelService.xpForLevel(level + 1);
        inventory.setItem(SLOT_LEVEL, MenuUtil.icon(Material.EXPERIENCE_BOTTLE,
                Component.text("✦ Level " + level, NamedTextColor.GREEN),
                List.of(Component.text((xp - xpForCurrent) + " / " + (xpForNext - xpForCurrent) + " XP to next level", NamedTextColor.GRAY),
                        Component.text("Earned from scaled-mob kills and", NamedTextColor.DARK_GRAY),
                        Component.text("discovering new zones.", NamedTextColor.DARK_GRAY))));

        inventory.setItem(SLOT_SETTINGS, MenuUtil.icon(Material.COMPARATOR,
                Component.text("⚙ Settings", NamedTextColor.AQUA),
                List.of(Component.text("Ambient effects, zone titles, and", NamedTextColor.GRAY),
                        Component.text("action bar vs. chat delivery.", NamedTextColor.GRAY))));

        if (viewer.hasPermission("rpgmood.admin")) {
            inventory.setItem(SLOT_ADMIN_CONFIG, MenuUtil.icon(Material.REDSTONE,
                    Component.text("🛠 Admin Config", NamedTextColor.RED),
                    List.of(Component.text("Spawn radius, mob curve, night/", NamedTextColor.GRAY),
                            Component.text("thunder bonus, weather effects.", NamedTextColor.GRAY))));
        }
    }

    @Override
    public void handleClick(Player player, int slot) {
        switch (slot) {
            case SLOT_JOURNAL -> {
                player.closeInventory();
                plugin.getDiarioCommand().openJournal(player);
            }
            case SLOT_ACHIEVEMENTS -> {
                AchievementsMenu achievements = new AchievementsMenu(plugin);
                achievements.renderFor(player);
                player.openInventory(achievements.getInventory());
            }
            case SLOT_ZONE -> {
                ZoneInfoMenu zoneInfo = new ZoneInfoMenu(plugin);
                zoneInfo.renderFor(player);
                player.openInventory(zoneInfo.getInventory());
            }
            case SLOT_LEADERBOARD -> {
                LeaderboardMenu leaderboard = new LeaderboardMenu(plugin);
                leaderboard.render();
                player.openInventory(leaderboard.getInventory());
            }
            case SLOT_FARMING -> {
                FarmingMenu farming = new FarmingMenu(plugin);
                farming.render();
                player.openInventory(farming.getInventory());
            }
            case SLOT_ANIMALS -> {
                AnimalsMenu animals = new AnimalsMenu(plugin);
                animals.renderFor(player);
                player.openInventory(animals.getInventory());
            }
            case SLOT_RPGLOOT -> {
                if (Bukkit.getPluginManager().getPlugin("RPGLoot") == null) return;
                RPGLootMenu rpgLootMenu = new RPGLootMenu(plugin);
                rpgLootMenu.renderFor(player);
                player.openInventory(rpgLootMenu.getInventory());
            }
            case SLOT_SETTINGS -> {
                SettingsMenu settings = new SettingsMenu(plugin);
                settings.renderFor(player);
                player.openInventory(settings.getInventory());
            }
            case SLOT_ADMIN_CONFIG -> {
                if (!player.hasPermission("rpgmood.admin")) return;
                AdminConfigMenu adminConfig = new AdminConfigMenu(plugin);
                adminConfig.render();
                player.openInventory(adminConfig.getInventory());
            }
            default -> { }
        }
    }
}
