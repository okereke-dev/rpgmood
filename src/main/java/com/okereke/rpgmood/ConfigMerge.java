package com.okereke.rpgmood;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Fills in any config keys a newer plugin version added into an existing user config file,
 * without touching any key the user already has a value for. This is Bukkit's own
 * YamlConfiguration defaults-layering mechanism ({@code setDefaults} + {@code copyDefaults}),
 * applied once at startup and actually written back to disk — {@code JavaPlugin#getConfig()}'s
 * built-in layering only exists in memory unless this is done, so new keys never show up in
 * the file on disk and get lost again on the next {@code saveConfig()}.
 *
 * Trade-off: {@link YamlConfiguration} doesn't preserve comments on save, so the first merge
 * of a given file strips whatever comments it had (the user's and the shipped file's). Every
 * value — the user's overrides and the newly-added defaults — is preserved exactly.
 */
final class ConfigMerge {

    private ConfigMerge() {}

    /**
     * Merges any new keys from the jar's bundled {@code fileName} into the user's copy in the
     * plugin's data folder, then saves. If the file doesn't exist yet, just creates it fresh
     * (same as a plain {@code saveResource}).
     */
    static void mergeAndSave(JavaPlugin plugin, String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
            return;
        }

        try (InputStream resource = plugin.getResource(fileName)) {
            if (resource == null) return;
            YamlConfiguration userConfig = YamlConfiguration.loadConfiguration(file);
            YamlConfiguration shippedDefaults = YamlConfiguration.loadConfiguration(new InputStreamReader(resource, StandardCharsets.UTF_8));
            userConfig.setDefaults(shippedDefaults);
            userConfig.options().copyDefaults(true);
            userConfig.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not merge new defaults into " + fileName + ": " + e.getMessage());
        }
    }
}
