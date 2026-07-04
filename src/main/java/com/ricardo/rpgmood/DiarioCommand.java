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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DiarioCommand implements CommandExecutor {

    private static final int ENTRIES_PER_PAGE = 5;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault());

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
        if (!player.hasPermission("rpgmood.player.diary")) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(plugin.getConfig().getString("messages.no_permission", "&cNo permission")));
            return true;
        }

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle("Adventure Journal");
        meta.setAuthor("RPGMood");

        List<PlayerJournalService.Entry> entries = plugin.getPlayerJournalService().getRecentEntries(player);
        if (entries.isEmpty()) {
            meta.addPage("Day 1: Your journey begins.\n\nYour first adventure has been recorded.");
        } else {
            for (int i = 0; i < entries.size(); i += ENTRIES_PER_PAGE) {
                StringBuilder page = new StringBuilder();
                int end = Math.min(i + ENTRIES_PER_PAGE, entries.size());
                for (int j = i; j < end; j++) {
                    PlayerJournalService.Entry entry = entries.get(j);
                    String timestamp = TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(entry.timeMillis()));
                    page.append(timestamp).append(" - ").append(entry.text());
                    if (j < end - 1) {
                        page.append("\n\n");
                    }
                }
                meta.addPage(page.toString());
            }
        }

        book.setItemMeta(meta);
        player.openBook(book);
        return true;
    }
}
