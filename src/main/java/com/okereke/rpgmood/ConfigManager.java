package com.okereke.rpgmood;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {

    private final RPGMoodPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration triggers;
    private FileConfiguration subzones;

    public ConfigManager(RPGMoodPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        ConfigMerge.mergeAndSave(plugin, "config.yml");
        plugin.reloadConfig();
        plugin.getConfig().options().copyDefaults(true);
        this.config = plugin.getConfig();

        ConfigMerge.mergeAndSave(plugin, "triggers.yml");
        ConfigMerge.mergeAndSave(plugin, "subzones.yml");
        File triggersFile = new File(plugin.getDataFolder(), "triggers.yml");
        File subzonesFile = new File(plugin.getDataFolder(), "subzones.yml");
        this.triggers = YamlConfiguration.loadConfiguration(triggersFile);
        this.subzones = YamlConfiguration.loadConfiguration(subzonesFile);
    }

    public FileConfiguration getConfigValues() {
        return config;
    }

    public FileConfiguration getTriggers() {
        return triggers;
    }

    public FileConfiguration getSubzones() {
        return subzones;
    }

    public void saveDefaultConfigs() {
        plugin.saveDefaultConfig();
        plugin.saveResource("triggers.yml", false);
        plugin.saveResource("subzones.yml", false);
    }

    /** Persists a single player toggle (effects/titles) to the config file without rewriting the entire document. */
    public void savePlayerToggle(String configKey, java.util.UUID playerId, boolean enabled) {
        String path = configKey + playerId;
        config.set(path, enabled);
        // Write the current config to disk
        plugin.saveConfig();
    }
}
