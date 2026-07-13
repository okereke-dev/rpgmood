package com.okereke.rpgmood.menu;

import com.okereke.rpgmood.PlayerStatsService;
import com.okereke.rpgmood.RPGMoodPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/** Top-10 leaderboard, click a category tab to switch — visual equivalent of /rpgmood leaderboard. */
public class LeaderboardMenu implements RPGMoodMenu {

    private enum Category { DEATHS, ZONES, LEVEL }

    private static final int SLOT_BACK = 0;
    private static final int SLOT_DEATHS = 2;
    private static final int SLOT_ZONES = 4;
    private static final int SLOT_LEVEL = 6;
    private static final int[] ENTRY_SLOTS = {9, 10, 11, 12, 13, 14, 15, 16, 17, 18};

    private final RPGMoodPlugin plugin;
    private final Inventory inventory;
    private Category selected = Category.DEATHS;

    public LeaderboardMenu(RPGMoodPlugin plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("✦ RPGMood — Leaderboard", NamedTextColor.GOLD));
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void render() {
        MenuUtil.fillBorder(inventory);
        inventory.setItem(SLOT_BACK, MenuUtil.icon(Material.ARROW, Component.text("← Back", NamedTextColor.GRAY)));
        inventory.setItem(SLOT_DEATHS, tab("Deaths", Material.SKELETON_SKULL, Category.DEATHS));
        inventory.setItem(SLOT_ZONES, tab("Zones Explored", Material.MAP, Category.ZONES));
        inventory.setItem(SLOT_LEVEL, tab("Highest Mob Level", Material.DIAMOND_SWORD, Category.LEVEL));

        for (int slot : ENTRY_SLOTS) inventory.setItem(slot, null);

        List<PlayerStatsService.StatEntry> top = switch (selected) {
            case DEATHS -> plugin.getPlayerStatsService().getTopDeaths(10);
            case ZONES -> plugin.getPlayerStatsService().getTopZoneChanges(10);
            case LEVEL -> plugin.getPlayerStatsService().getTopMobLevel(10);
        };

        for (int i = 0; i < top.size() && i < ENTRY_SLOTS.length; i++) {
            PlayerStatsService.StatEntry entry = top.get(i);
            inventory.setItem(ENTRY_SLOTS[i], MenuUtil.icon(Material.PLAYER_HEAD,
                    Component.text((i + 1) + ". " + entry.name(), NamedTextColor.WHITE),
                    List.of(Component.text(String.valueOf(entry.value()), NamedTextColor.YELLOW))));
        }
        if (top.isEmpty()) {
            inventory.setItem(ENTRY_SLOTS[0], MenuUtil.icon(Material.BARRIER, Component.text("No data yet", NamedTextColor.GRAY)));
        }
    }

    private ItemStack tab(String name, Material material, Category category) {
        boolean isSelected = category == selected;
        return MenuUtil.icon(material, Component.text((isSelected ? "▶ " : "") + name, isSelected ? NamedTextColor.YELLOW : NamedTextColor.GRAY));
    }

    @Override
    public void handleClick(Player player, int slot) {
        if (slot == SLOT_BACK) {
            player.openInventory(new MainMenu(plugin, player).getInventory());
            return;
        }
        Category clicked = switch (slot) {
            case SLOT_DEATHS -> Category.DEATHS;
            case SLOT_ZONES -> Category.ZONES;
            case SLOT_LEVEL -> Category.LEVEL;
            default -> null;
        };
        if (clicked != null) {
            selected = clicked;
            render();
        }
    }
}
