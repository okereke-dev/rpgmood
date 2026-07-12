package com.ricardo.rpgmood.menu;

import com.ricardo.rpgmood.RPGLootIntegration;
import com.ricardo.rpgmood.RPGMoodPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only snapshot of the viewer's RPGLoot standing — equipped rarities, active set, and
 * lifetime stats. Entirely soft: reads RPGLoot's PDC tags (same as the achievement hooks) and
 * its playerstats.yml file directly, no compile-time dependency. Only reachable from the main
 * menu when RPGLoot is detected installed.
 */
public class RPGLootMenu implements RPGMoodMenu {

    private static final int SLOT_BACK = 0;
    private static final int SLOT_HELMET = 11;
    private static final int SLOT_CHEST = 12;
    private static final int SLOT_LEGS = 13;
    private static final int SLOT_BOOTS = 14;
    private static final int SLOT_HAND = 15;
    private static final int SLOT_SET = 20;
    private static final int SLOT_STATS = 24;

    private final RPGMoodPlugin plugin;
    private final Inventory inventory;

    public RPGLootMenu(RPGMoodPlugin plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("✦ RPGMood — RPGLoot", NamedTextColor.GOLD));
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void renderFor(Player player) {
        MenuUtil.fillBorder(inventory);
        inventory.setItem(SLOT_BACK, MenuUtil.icon(Material.ARROW, Component.text("← Back", NamedTextColor.GRAY)));

        PlayerInventory inv = player.getInventory();
        inventory.setItem(SLOT_HELMET, equipmentIcon(Material.LEATHER_HELMET, "Helmet", inv.getHelmet()));
        inventory.setItem(SLOT_CHEST, equipmentIcon(Material.LEATHER_CHESTPLATE, "Chestplate", inv.getChestplate()));
        inventory.setItem(SLOT_LEGS, equipmentIcon(Material.LEATHER_LEGGINGS, "Leggings", inv.getLeggings()));
        inventory.setItem(SLOT_BOOTS, equipmentIcon(Material.LEATHER_BOOTS, "Boots", inv.getBoots()));
        inventory.setItem(SLOT_HAND, equipmentIcon(Material.WOODEN_SWORD, "Held item", inv.getItemInMainHand()));

        String activeSetRarity = RPGLootIntegration.getActiveSetRarity(player);
        String setName = guessActiveSetName(inv);
        if (activeSetRarity != null) {
            inventory.setItem(SLOT_SET, MenuUtil.icon(Material.SHIELD,
                    Component.text("Active Set" + (setName != null ? ": " + setName : ""), NamedTextColor.AQUA),
                    List.of(Component.text(activeSetRarity + " tier, full 5 pieces", NamedTextColor.GRAY))));
        } else {
            inventory.setItem(SLOT_SET, MenuUtil.icon(Material.SHIELD,
                    Component.text("No active set", NamedTextColor.DARK_GRAY),
                    List.of(Component.text("Equip 2+ matching pieces for a set bonus.", NamedTextColor.DARK_GRAY))));
        }

        RPGLootIntegration.LootStats stats = RPGLootIntegration.readPlayerStats(player.getUniqueId());
        if (stats != null) {
            inventory.setItem(SLOT_STATS, MenuUtil.icon(Material.NETHERITE_INGOT,
                    Component.text("Lifetime Stats", NamedTextColor.YELLOW),
                    List.of(
                            Component.text("Legendaries found: " + stats.legendariesFound(), NamedTextColor.GOLD),
                            Component.text("Sets completed: " + stats.setsCompleted(), NamedTextColor.AQUA),
                            Component.text("Artifacts found: " + stats.artifactsFound() + "/4", NamedTextColor.LIGHT_PURPLE)
                    )));
        }
    }

    private ItemStack equipmentIcon(Material fallback, String slotName, ItemStack equipped) {
        String rarity = RPGLootIntegration.getRarity(equipped);
        if (rarity == null) {
            return MenuUtil.icon(fallback, Component.text(slotName, NamedTextColor.DARK_GRAY),
                    List.of(Component.text("No RPGLoot item equipped", NamedTextColor.DARK_GRAY)));
        }
        Material shown = equipped.getType();
        return MenuUtil.icon(shown, Component.text(slotName + ": " + rarity, rarityColor(rarity)));
    }

    private NamedTextColor rarityColor(String rarity) {
        return switch (rarity) {
            case "LEGENDARY" -> NamedTextColor.GOLD;
            case "HERO" -> NamedTextColor.GREEN;
            case "RARE" -> NamedTextColor.LIGHT_PURPLE;
            case "UNCOMMON" -> NamedTextColor.YELLOW;
            default -> NamedTextColor.GRAY;
        };
    }

    /** Best-effort: most common set name among equipped pieces (RPGLootIntegration only exposes the active set's rarity, not its name). */
    private String guessActiveSetName(PlayerInventory inv) {
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack item : new ItemStack[]{inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots(), inv.getItemInMainHand()}) {
            String setName = RPGLootIntegration.getSetName(item);
            if (setName != null) counts.merge(setName, 1, Integer::sum);
        }
        return counts.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(null);
    }

    @Override
    public void handleClick(Player player, int slot) {
        if (slot == SLOT_BACK) {
            player.openInventory(new MainMenu(plugin, player).getInventory());
        }
    }
}
