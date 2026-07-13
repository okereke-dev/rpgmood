package com.okereke.rpgmood;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Soft integration with RPGLoot: reacts to items the player picks up or equips that carry
 * RPGLoot's PersistentDataContainer tags (see {@link RPGLootIntegration}). Entirely inert if
 * RPGLoot isn't installed — the tags simply never exist on any ItemStack, so every check here
 * is a harmless no-op.
 *
 * Equipment-change detection mirrors RPGLoot's own SetListener (InventoryClickEvent on
 * armor/player-inv/quickbar slots + PlayerItemHeldEvent + a short delay so the click has
 * actually applied before we re-read equipment), since there's no compile-time dependency
 * on RPGLoot to reuse its listener directly.
 */
public class RPGLootAchievementListener implements Listener {

    private final RPGMoodPlugin plugin;
    private final Map<UUID, Integer> pendingTasks = new HashMap<>();

    public RPGLootAchievementListener(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        plugin.getAchievementManager().onRPGLootItemFound(player, event.getItem().getItemStack());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        boolean isArmorSlot = event.getSlotType() == InventoryType.SlotType.ARMOR;
        boolean isPlayerInv = event.getView().getType() == InventoryType.CRAFTING;
        boolean isQuickBar = event.getSlotType() == InventoryType.SlotType.QUICKBAR;
        if (!isArmorSlot && !isPlayerInv && !isQuickBar) {
            return;
        }
        scheduleEquipmentCheck(player, 1L);
    }

    @EventHandler
    public void onHeldItemChange(PlayerItemHeldEvent event) {
        scheduleEquipmentCheck(event.getPlayer(), 1L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        scheduleEquipmentCheck(event.getPlayer(), 5L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Integer taskId = pendingTasks.remove(event.getPlayer().getUniqueId());
        if (taskId != null) {
            plugin.getServer().getScheduler().cancelTask(taskId);
        }
    }

    private void scheduleEquipmentCheck(Player player, long delayTicks) {
        UUID uuid = player.getUniqueId();
        Integer existing = pendingTasks.remove(uuid);
        if (existing != null) {
            plugin.getServer().getScheduler().cancelTask(existing);
        }
        int taskId = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            pendingTasks.remove(uuid);
            if (player.isOnline()) {
                checkEquipment(player);
            }
        }, delayTicks).getTaskId();
        pendingTasks.put(uuid, taskId);
    }

    private void checkEquipment(Player player) {
        plugin.getAchievementManager().onEquipmentChanged(player);

        PlayerInventory inv = player.getInventory();
        for (ItemStack item : new ItemStack[]{inv.getHelmet(), inv.getChestplate(), inv.getLeggings(), inv.getBoots(), inv.getItemInMainHand()}) {
            plugin.getAchievementManager().onRPGLootItemFound(player, item);
        }
    }
}
