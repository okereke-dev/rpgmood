package com.ricardo.rpgmood;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persists a rolling log of each player's recent adventures (zone changes, deaths) for /diary. */
public class PlayerJournalService {

    private static final int MAX_ENTRIES_PER_PLAYER = 50;

    private final RPGMoodPlugin plugin;
    private final File journalFolder;

    public PlayerJournalService(RPGMoodPlugin plugin) {
        this.plugin = plugin;
        this.journalFolder = new File(plugin.getDataFolder(), "playerdata");
        if (!journalFolder.exists()) {
            journalFolder.mkdirs();
        }
    }

    public void addEntry(Player player, String text) {
        if (player == null || text == null || text.isBlank()) {
            return;
        }

        File file = fileFor(player.getUniqueId());
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Map<String, Object>> entries = readEntries(config);

        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("time", System.currentTimeMillis());
        entry.put("text", text);
        entries.add(entry);

        while (entries.size() > MAX_ENTRIES_PER_PLAYER) {
            entries.remove(0);
        }

        config.set("entries", entries);
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save journal for " + player.getName() + ": " + e.getMessage());
        }
    }

    /** Most recent entries first. */
    public List<Entry> getRecentEntries(Player player) {
        File file = fileFor(player.getUniqueId());
        if (!file.exists()) {
            return List.of();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Map<String, Object>> entries = readEntries(config);
        List<Entry> result = new ArrayList<>(entries.size());
        for (int i = entries.size() - 1; i >= 0; i--) {
            Object text = entries.get(i).get("text");
            Object time = entries.get(i).get("time");
            if (text != null) {
                long millis = time instanceof Number number ? number.longValue() : System.currentTimeMillis();
                result.add(new Entry(millis, String.valueOf(text)));
            }
        }
        return result;
    }

    public record Entry(long timeMillis, String text) {}

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readEntries(YamlConfiguration config) {
        List<Map<?, ?>> raw = config.getMapList("entries");
        List<Map<String, Object>> entries = new ArrayList<>(raw.size());
        for (Map<?, ?> map : raw) {
            entries.add((Map<String, Object>) map);
        }
        return entries;
    }

    private File fileFor(UUID id) {
        return new File(journalFolder, id + ".yml");
    }
}
