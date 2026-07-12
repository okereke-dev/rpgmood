package com.ricardo.rpgmood;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

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

    private static String readItemTag(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }
}
