package com.ricardo.rpgmood.menu;

import com.ricardo.rpgmood.AchievementManager;
import com.ricardo.rpgmood.RPGMoodPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/** Paginated-by-category achievement browser — the visual equivalent of /rpgmood achievements, without the wall of chat text. */
public class AchievementsMenu implements RPGMoodMenu {

    private static final int SLOT_BACK = 0;
    private static final int[] CATEGORY_SLOTS = {2, 3, 4, 5, 6};
    private static final int[] ACHIEVEMENT_SLOTS = {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26
    };

    private static final Map<AchievementManager.Category, Material> CATEGORY_ICONS = Map.of(
            AchievementManager.Category.EXPLORATION, Material.COMPASS,
            AchievementManager.Category.COMBAT, Material.IRON_SWORD,
            AchievementManager.Category.SURVIVAL, Material.TOTEM_OF_UNDYING,
            AchievementManager.Category.FARMING, Material.WHEAT,
            AchievementManager.Category.LOOT, Material.CHEST
    );

    private final RPGMoodPlugin plugin;
    private final Inventory inventory;
    private AchievementManager.Category selectedCategory;

    public AchievementsMenu(RPGMoodPlugin plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 54, Component.text("✦ RPGMood — Achievements", NamedTextColor.GOLD));
        this.selectedCategory = AchievementManager.Category.EXPLORATION;
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void renderFor(Player player) {
        MenuUtil.fillBorder(inventory);
        inventory.setItem(SLOT_BACK, MenuUtil.icon(Material.ARROW, Component.text("← Back", NamedTextColor.GRAY)));

        AchievementManager.Category[] categories = AchievementManager.Category.values();
        for (int i = 0; i < categories.length && i < CATEGORY_SLOTS.length; i++) {
            inventory.setItem(CATEGORY_SLOTS[i], categoryTab(player, categories[i]));
        }

        for (int slot : ACHIEVEMENT_SLOTS) {
            inventory.setItem(slot, null);
        }

        List<AchievementManager.Achievement> inCategory = AchievementManager.ALL_ACHIEVEMENTS.stream()
                .filter(a -> a.category() == selectedCategory)
                .toList();
        AchievementManager manager = plugin.getAchievementManager();
        for (int i = 0; i < inCategory.size() && i < ACHIEVEMENT_SLOTS.length; i++) {
            inventory.setItem(ACHIEVEMENT_SLOTS[i], achievementIcon(manager, player, inCategory.get(i)));
        }
    }

    private ItemStack categoryTab(Player player, AchievementManager.Category category) {
        AchievementManager manager = plugin.getAchievementManager();
        long total = AchievementManager.ALL_ACHIEVEMENTS.stream().filter(a -> a.category() == category).count();
        long unlocked = AchievementManager.ALL_ACHIEVEMENTS.stream()
                .filter(a -> a.category() == category && manager.hasUnlocked(player, a.id()))
                .count();

        boolean selected = category == selectedCategory;
        String name = capitalize(category.name());
        return MenuUtil.icon(CATEGORY_ICONS.getOrDefault(category, Material.PAPER),
                Component.text((selected ? "▶ " : "") + name, selected ? NamedTextColor.YELLOW : NamedTextColor.GRAY),
                List.of(Component.text(unlocked + "/" + total + " unlocked", NamedTextColor.DARK_GRAY)));
    }

    private ItemStack achievementIcon(AchievementManager manager, Player player, AchievementManager.Achievement ach) {
        boolean unlocked = manager.hasUnlocked(player, ach.id());
        if (unlocked) {
            return MenuUtil.icon(Material.LIME_DYE,
                    Component.text("✅ " + ach.icon() + " " + ach.name(), NamedTextColor.GREEN),
                    List.of(Component.text(ach.description(), NamedTextColor.GRAY)));
        }
        return MenuUtil.icon(Material.GRAY_DYE,
                Component.text("⬛ " + ach.icon() + " " + ach.name(), NamedTextColor.DARK_GRAY),
                List.of(Component.text("Not yet unlocked", NamedTextColor.DARK_GRAY)));
    }

    private static String capitalize(String enumName) {
        String lower = enumName.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    @Override
    public void handleClick(Player player, int slot) {
        if (slot == SLOT_BACK) {
            player.openInventory(new MainMenu(plugin, player).getInventory());
            return;
        }

        AchievementManager.Category[] categories = AchievementManager.Category.values();
        for (int i = 0; i < CATEGORY_SLOTS.length; i++) {
            if (CATEGORY_SLOTS[i] == slot && i < categories.length) {
                selectedCategory = categories[i];
                renderFor(player);
                return;
            }
        }
    }
}
