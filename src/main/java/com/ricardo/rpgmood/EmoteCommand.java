package com.ricardo.rpgmood;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EmoteCommand implements CommandExecutor {

    private final RPGMoodPlugin plugin;

    public EmoteCommand(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("rpgmood.player.emotes")) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfig().getString("messages.no_permission", "&cNo permission")));
            return true;
        }

        String emoteName = label.toLowerCase();
        player.getWorld().spawnParticle(org.bukkit.Particle.HEART, player.getLocation().add(0, 1.5, 0), 8, 0.2, 0.2, 0.2, 0.01);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.sendMessage(Component.text("Emote ").color(NamedTextColor.GREEN)
                .append(Component.text(emoteName + " executed.").color(NamedTextColor.GREEN)));
        return true;
    }
}
