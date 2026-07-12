package com.ricardo.rpgmood;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.UUID;

/**
 * Optional integration with RPGLoot: reads its plain, cross-plugin PersistentDataContainer
 * conventions directly — no dependency on RPGLoot's classes, no presence check needed, and
 * no effect at all if RPGLoot isn't installed (the tags simply won't exist). Mirrors
 * RPGLoot's own {@code RPGMoodIntegration} class, which reads RPGMood's {@code rpgmood:level}
 * tag the same way.
 */
public final class RPGLootIntegration {

    private static final NamespacedKey RARITY_KEY = new NamespacedKey("rpgloot", "rarity");
    private static final NamespacedKey ARTIFACT_ID_KEY = new NamespacedKey("rpgloot", "artifact_id");
    private static final NamespacedKey SET_NAME_KEY = new NamespacedKey("rpgloot", "set_name");
    private static final NamespacedKey ACTIVE_SET_RARITY_KEY = new NamespacedKey("rpgloot", "active_set_rarity");

    private RPGLootIntegration() {}

    /** The item's RPGLoot rarity name (e.g. "LEGENDARY"), or null if it's not an RPGLoot item. */
    public static String getRarity(ItemStack item) {
        return readItemTag(item, RARITY_KEY);
    }

    /** The item's RPGLoot artifact id (e.g. "WARDENS_MAUL"), or null if it isn't an Artifact. */
    public static String getArtifactId(ItemStack item) {
        return readItemTag(item, ARTIFACT_ID_KEY);
    }

    /** The item's RPGLoot set name (e.g. "Shadowveil"), or null if it isn't part of a set. */
    public static String getSetName(ItemStack item) {
        return readItemTag(item, SET_NAME_KEY);
    }

    /**
     * The rarity of the player's currently active full (5-piece) RPGLoot set, or null if no
     * full set is active. Written on the player by RPGLoot's SetTracker.
     */
    public static String getActiveSetRarity(Player player) {
        return player.getPersistentDataContainer().get(ACTIVE_SET_RARITY_KEY, PersistentDataType.STRING);
    }

    /** Lifetime loot stats read directly from RPGLoot's own playerstats.yml — see {@link #readPlayerStats}. */
    public record LootStats(int legendariesFound, int setsCompleted, int artifactsFound) {}

    /**
     * Reads a player's lifetime RPGLoot stats straight from RPGLoot's data file. This is
     * reading another plugin's private file, not an API — there's no dependency on RPGLoot's
     * classes, but if RPGLoot ever changes that file's format this stops reflecting reality
     * (fails safe: returns null, callers should treat that as "no data available").
     * Returns null if RPGLoot isn't installed.
     */
    public static LootStats readPlayerStats(UUID uuid) {
        Plugin rpgLoot = Bukkit.getPluginManager().getPlugin("RPGLoot");
        if (rpgLoot == null) return null;

        File file = new File(rpgLoot.getDataFolder(), "playerstats.yml");
        if (!file.exists()) return new LootStats(0, 0, 0);

        try {
            YamlConfiguration data = YamlConfiguration.loadConfiguration(file);
            String path = uuid + ".";
            int legendaries = data.getInt(path + "legendaries-found", 0);
            int sets = data.getInt(path + "sets-completed", 0);
            int artifacts = data.getStringList(path + "artifacts-found").size();
            return new LootStats(legendaries, sets, artifacts);
        } catch (Exception e) {
            return new LootStats(0, 0, 0);
        }
    }

    private static String readItemTag(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }
}
