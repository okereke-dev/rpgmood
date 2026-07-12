package com.ricardo.rpgmood.farming.listener;

import com.ricardo.rpgmood.RPGMoodPlugin;
import com.ricardo.rpgmood.farming.CropManager;
import com.ricardo.rpgmood.farming.CropQuality;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;

/**
 * Listens for crop planting, growth, and harvest interactions.
 * Integrates with CropManager for quality-based harvests.
 */
public class CropListener implements Listener {

    private final RPGMoodPlugin plugin;

    public CropListener(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Prevent breaking mature crops with bare hands — forces using the harvest system.
     * Also handles the actual harvest via right-click or break.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (event.isCancelled()) return;

        Block block = event.getBlock();
        CropManager cropManager = plugin.getCropManager();
        if (cropManager == null) return;
        if (!cropManager.isTrackedCrop(block)) return;

        Player player = event.getPlayer();
        event.setCancelled(true); // cancel normal break, we handle it

        if (!cropManager.isMature(block)) {
            plugin.getMessageService().send(player, Component.text("This crop isn't ready yet.").color(NamedTextColor.YELLOW));
            return;
        }

        // Harvest with quality system
        List<ItemStack> drops = cropManager.harvestCrop(block, player);
        for (ItemStack drop : drops) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), drop);
        }

        // Reset to air (remove the crop)
        block.setType(Material.AIR);

        plugin.getMessageService().send(player, Component.text("Harvested!").color(NamedTextColor.GREEN));
    }

    /**
     * Detect when a player right-clicks a crop to check its status.
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        CropManager cropManager = plugin.getCropManager();
        if (cropManager == null) return;
        if (!cropManager.isTrackedCrop(block)) return;

        Player player = event.getPlayer();

        // If holding seeds, let vanilla planting happen
        ItemStack hand = event.getItem();
        if (hand != null && cropManager.isSeed(hand.getType())) return;

        event.setCancelled(true);

        if (cropManager.isMature(block)) {
            // Harvest by right-click (like vanilla)
            List<ItemStack> drops = cropManager.harvestCrop(block, player);
            for (ItemStack drop : drops) {
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), drop);
            }
            block.setType(Material.AIR);
            plugin.getMessageService().send(player, Component.text("Harvested!").color(NamedTextColor.GREEN));
        } else {
            // Show quality preview for growing crop
            double score = cropManager.calculateQualityScore(block.getLocation(), player);
            CropQuality quality = cropManager.scoreToQuality(score);
            plugin.getMessageService().send(player, Component.text()
                    .append(Component.text("Growing... ", NamedTextColor.GRAY))
                    .append(Component.text("Predicted quality: ", NamedTextColor.WHITE))
                    .append(Component.text(quality.getDisplayName()))
                    .build());
        }
    }

    /**
     * Log planting of crops to journal.
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.isCancelled()) return;

        CropManager cropManager = plugin.getCropManager();
        if (cropManager == null) return;
        if (!cropManager.isTrackedCrop(event.getBlock())) return;

        Player player = event.getPlayer();
        String cropName = event.getBlock().getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        plugin.getPlayerJournalService().addEntry(player,
                "Planted " + cropName + " in " + plugin.getZoneManager().getCurrentZoneDisplayName(player));
    }
}
