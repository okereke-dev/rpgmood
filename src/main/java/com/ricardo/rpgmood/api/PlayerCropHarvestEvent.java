package com.ricardo.rpgmood.api;

import com.ricardo.rpgmood.farming.CropQuality;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Called when a player harvests a crop in the RPGMood farming system.
 * Exposes the crop type, quality, location, and harvested items.
 */
public class PlayerCropHarvestEvent extends PlayerEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String cropType;
    private final CropQuality quality;
    private final Location location;
    private final List<ItemStack> drops;

    public PlayerCropHarvestEvent(@NotNull Player player, String cropType, CropQuality quality,
                                  Location location, List<ItemStack> drops) {
        super(player);
        this.cropType = cropType;
        this.quality = quality;
        this.location = location;
        this.drops = drops;
    }

    /** The configured crop type key (e.g. "carrot", "tomato"). */
    public String getCropType() { return cropType; }

    /** The quality level of the harvested crop (BRONZE, SILVER, GOLD). */
    public CropQuality getQuality() { return quality; }

    /** The location where the crop was harvested. */
    public Location getLocation() { return location; }

    /** The items that will be dropped. Modify this list to alter drops. */
    public List<ItemStack> getDrops() { return drops; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static HandlerList getHandlerList() { return HANDLERS; }
}
