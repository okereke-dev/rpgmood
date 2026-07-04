package com.ricardo.rpgmood;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class RPGMoodCommand implements CommandExecutor {

    private final RPGMoodPlugin plugin;

    public RPGMoodCommand(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /rpgmood reload|toggle|info").color(NamedTextColor.RED));
            return true;
        }

        if (args[0].equalsIgnoreCase("info")) {
            return handleInfo(sender);
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("rpgmood.admin")) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfig().getString("messages.no_permission", "&cNo permission")));
                return true;
            }
            plugin.getConfigManager().reload();
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfig().getString("messages.plugin_reloaded", "&aReloaded")));
            return true;
        }

        if (args[0].equalsIgnoreCase("toggle")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
                return true;
            }
            if (!player.hasPermission("rpgmood.player.toggle")) {
                sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfig().getString("messages.no_permission", "&cNo permission")));
                return true;
            }
            boolean enabled = !plugin.getConfigManager().getConfigValues().getBoolean("player_effects." + player.getUniqueId(), true);
            plugin.getConfig().set("player_effects." + player.getUniqueId(), enabled);
            plugin.saveConfig();
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(enabled ? plugin.getConfig().getString("messages.toggle_enabled", "&aEnabled") : plugin.getConfig().getString("messages.toggle_disabled", "&eDisabled")));
            return true;
        }

        sender.sendMessage(Component.text("Usage: /rpgmood reload|toggle|info").color(NamedTextColor.RED));
        return true;
    }

    /** Admin/debug view of the zone and mob difficulty at the sender's current location. */
    private boolean handleInfo(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("rpgmood.admin")) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfig().getString("messages.no_permission", "&cNo permission")));
            return true;
        }

        String zone = plugin.getZoneManager().getCurrentZoneDisplayName(player);
        String biome = player.getLocation().getBlock().getBiome().name();
        double distanceFromSpawn = player.getLocation().distance(player.getWorld().getSpawnLocation());

        player.sendMessage(Component.text("=== RPGMood Info ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("Zone: ").color(NamedTextColor.GRAY).append(Component.text(zone).color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Biome: ").color(NamedTextColor.GRAY).append(Component.text(biome).color(NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Distance from spawn: ").color(NamedTextColor.GRAY).append(Component.text((int) distanceFromSpawn + " blocks").color(NamedTextColor.WHITE)));

        for (EntityType type : new EntityType[]{EntityType.ZOMBIE, EntityType.SKELETON, EntityType.ENDERMAN}) {
            int baseLevel = plugin.getMobScalingService().getBaseLevel(type);
            int level = plugin.getMobScalingService().calculateLevelAt(player.getLocation(), baseLevel);
            player.sendMessage(Component.text(type.name() + " would spawn at level: ").color(NamedTextColor.GRAY).append(Component.text(level).color(NamedTextColor.YELLOW)));
        }
        return true;
    }
}
