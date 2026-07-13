package com.okereke.rpgmood.menu;

import com.okereke.rpgmood.RPGMoodPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

/** Read-only snapshot of the viewer's current zone and local mob danger — open to every player, unlike the admin-only /rpgmood info command. */
public class ZoneInfoMenu implements RPGMoodMenu {

    private static final int SLOT_BACK = 0;
    private static final int SLOT_ZONE = 11;
    private static final int SLOT_DISTANCE = 13;
    private static final int SLOT_DANGER = 15;

    private final RPGMoodPlugin plugin;
    private final Inventory inventory;

    public ZoneInfoMenu(RPGMoodPlugin plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("✦ RPGMood — Current Zone", NamedTextColor.GOLD));
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void renderFor(Player player) {
        MenuUtil.fillBorder(inventory);
        inventory.setItem(SLOT_BACK, MenuUtil.icon(Material.ARROW, Component.text("← Back", NamedTextColor.GRAY)));

        String zone = plugin.getZoneManager().getCurrentZoneDisplayName(player);
        String biome = player.getLocation().getBlock().getBiome().name();
        double distance = player.getLocation().distance(player.getWorld().getSpawnLocation());

        inventory.setItem(SLOT_ZONE, MenuUtil.icon(Material.COMPASS,
                Component.text(zone, NamedTextColor.YELLOW),
                List.of(Component.text("Biome: " + biome, NamedTextColor.GRAY))));

        inventory.setItem(SLOT_DISTANCE, MenuUtil.icon(Material.MAP,
                Component.text("Distance from spawn", NamedTextColor.AQUA),
                List.of(Component.text((int) distance + " blocks", NamedTextColor.GRAY))));

        List<Component> dangerLore = new java.util.ArrayList<>();
        for (EntityType type : new EntityType[]{EntityType.ZOMBIE, EntityType.SKELETON, EntityType.ENDERMAN}) {
            int baseLevel = plugin.getMobScalingService().getBaseLevel(type);
            int level = plugin.getMobScalingService().calculateLevelAt(player.getLocation(), baseLevel);
            dangerLore.add(Component.text(capitalize(type.name()) + ": Lv. " + level, NamedTextColor.GRAY));
        }
        inventory.setItem(SLOT_DANGER, MenuUtil.icon(Material.IRON_SWORD,
                Component.text("Local danger level", NamedTextColor.RED), dangerLore));
    }

    private static String capitalize(String enumName) {
        String lower = enumName.toLowerCase(java.util.Locale.ROOT).replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    @Override
    public void handleClick(Player player, int slot) {
        if (slot == SLOT_BACK) {
            player.openInventory(new MainMenu(plugin, player).getInventory());
        }
    }
}
