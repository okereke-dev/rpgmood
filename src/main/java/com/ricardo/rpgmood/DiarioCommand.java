package com.ricardo.rpgmood;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class DiarioCommand implements CommandExecutor, TabCompleter {

    private static final int MAX_PAGE_LENGTH = 245; // Safely under Minecraft's 256-char page limit
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
        openJournal(player);
        return true;
    }

    /** Builds and opens the player's adventure journal book. Shared by the /diary command and the RPGMood menu. */
    public void openJournal(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle("Adventure Journal");
        meta.setAuthor("RPGMood");

        List<PlayerJournalService.Entry> entries = plugin.getPlayerJournalService().getRecentEntries(player);
        if (entries.isEmpty()) {
            meta.addPage("Day 1: Your journey begins.\n\nYour first adventure has been recorded.");
        } else {
            for (String page : buildPages(entries)) {
                meta.addPage(page);
            }
        }

        book.setItemMeta(meta);
        player.openBook(book);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return List.of();
    }

    /** Builds book pages respecting Minecraft's character limit, splitting entries across pages as needed. */
    private List<String> buildPages(List<PlayerJournalService.Entry> entries) {
        List<String> pages = new ArrayList<>();
        StringBuilder currentPage = new StringBuilder();

        for (int i = entries.size() - 1; i >= 0; i--) {
            PlayerJournalService.Entry entry = entries.get(i);
            String timestamp = TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(entry.timeMillis()));
            String line = timestamp + " - " + entry.text();

            // If the line itself is too long for a single page, truncate it
            String truncated = line.length() > MAX_PAGE_LENGTH
                    ? line.substring(0, MAX_PAGE_LENGTH - 3) + "..."
                    : line;

            // If adding this line would exceed the page limit, start a new page
            if (!currentPage.isEmpty() && currentPage.length() + truncated.length() + 2 > MAX_PAGE_LENGTH) {
                pages.add(currentPage.toString());
                currentPage = new StringBuilder();
            }

            if (!currentPage.isEmpty()) {
                currentPage.append("\n\n");
            }
            currentPage.append(truncated);
        }

        if (!currentPage.isEmpty()) {
            pages.add(currentPage.toString());
        }

        return pages;
    }
}
