package com.ricardo.rpgmood.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

/** Shared building blocks for RPGMood GUI menus, so each menu class only has to describe its own content. */
final class MenuUtil {

    private MenuUtil() {}

    /** Fills the first and last rows of the inventory with a blank glass pane border. */
    static void fillBorder(Inventory inventory) {
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.text(" "));
        pane.setItemMeta(meta);

        int size = inventory.getSize();
        for (int i = 0; i < 9 && i < size; i++) inventory.setItem(i, pane);
        for (int i = size - 9; i < size; i++) if (i >= 0) inventory.setItem(i, pane);
    }

    /** Builds a display icon with a non-italic name and lore (Minecraft italicizes item lore/names by default, which reads oddly for UI text). */
    static ItemStack icon(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        item.setItemMeta(meta);
        return item;
    }

    static ItemStack icon(Material material, Component name) {
        return icon(material, name, List.of());
    }
}
