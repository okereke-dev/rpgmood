package com.okereke.rpgmood;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final RPGMoodPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration zones;
    private FileConfiguration triggers;

    public ConfigManager(RPGMoodPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();
        File zonesFile = new File(plugin.getDataFolder(), "zones.yml");
        if (!zonesFile.exists()) {
            plugin.saveResource("zones.yml", false);
        }
        File triggersFile = new File(plugin.getDataFolder(), "triggers.yml");
        if (!triggersFile.exists()) {
            plugin.saveResource("triggers.yml", false);
        }
        this.zones = YamlConfiguration.loadConfiguration(zonesFile);
        this.triggers = YamlConfiguration.loadConfiguration(triggersFile);
    }

    public FileConfiguration getConfigValues() {
        return config;
    }

    public FileConfiguration getZones() {
        return zones;
    }

    public FileConfiguration getTriggers() {
        return triggers;
    }

    public void saveDefaultConfigs() {
        plugin.saveDefaultConfig();
        plugin.saveResource("zones.yml", false);
        plugin.saveResource("triggers.yml", false);
    }

    /** Persists a single player toggle (effects/titles) to the config file without rewriting the entire document. */
    public void savePlayerToggle(String configKey, java.util.UUID playerId, boolean enabled) {
        String path = configKey + playerId;
        config.set(path, enabled);
        // Write the current config to disk
        plugin.saveConfig();
    }
}
