package com.okereke.rpgmood.menu;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Single shared listener for every RPGMood GUI menu. Read-only inventories — all clicks and
 * drags are cancelled, clicks in the menu itself are routed to {@link RPGMoodMenu#handleClick}.
 */
public class RPGMoodMenuListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof RPGMoodMenu menu)) return;
        event.setCancelled(true);

        if (event.getClickedInventory() != event.getView().getTopInventory()) return;
        if (event.getWhoClicked() instanceof Player player) {
            menu.handleClick(player, event.getSlot());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof RPGMoodMenu) {
            event.setCancelled(true);
        }
    }
}
