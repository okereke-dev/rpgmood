package com.okereke.rpgmood.menu;

import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

/**
 * Common contract for every RPGMood inventory-GUI screen. A single shared
 * {@link RPGMoodMenuListener} routes clicks to whichever menu is open by delegating to
 * {@link #handleClick}, so new menus never need their own click listener.
 */
public interface RPGMoodMenu extends InventoryHolder {

    /** Called when the viewer clicks a slot in this menu's own inventory (not their own inventory below it). */
    void handleClick(Player player, int slot);
}
