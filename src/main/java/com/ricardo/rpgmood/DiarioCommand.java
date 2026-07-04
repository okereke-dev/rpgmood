package com.ricardo.rpgmood;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public class DiarioCommand implements CommandExecutor {

    private final RPGMoodPlugin plugin;

    public DiarioCommand(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }
        if (!player.hasPermission("rpgmood.player.diario")) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfig().getString("messages.no_permission", "&cNo permission")));
            return true;
        }

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle("Adventure Journal");
        meta.setAuthor("RPGMood");
        meta.addPage("Day 1: Your journey begins.\n\nYour first adventure has been recorded.");
        meta.addPage("Day 2: You discovered new regions.\n\nThe atmosphere of the world responds to your steps.");
        book.setItemMeta(meta);
        player.openBook(book);
        return true;
    }
}
