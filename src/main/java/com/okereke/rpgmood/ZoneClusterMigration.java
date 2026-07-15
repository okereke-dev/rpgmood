package com.okereke.rpgmood;

import java.io.File;

/**
 * One-shot Phase 2 upgrade step. Before organic zone clusters existed, unclaimed territory used
 * a "DYNAMIC_ZONE|biome|world|regionX|regionZ" key that reset on every server restart; players
 * could still have that prefix recorded in their zone-discovery list and achievement progress.
 * On the first boot after this ships, strips those legacy entries out before the cluster system
 * is used for the first time — curated zones.yml-backed entries are untouched in both files, and
 * already-unlocked achievements are never revoked (see AchievementManager.purgeLegacyDynamicZoneProgress).
 */
final class ZoneClusterMigration {

    private ZoneClusterMigration() {}

    static void migrateIfNeeded(RPGMoodPlugin plugin) {
        File clustersFile = new File(plugin.getDataFolder(), "dynamiczones.yml");
        if (clustersFile.exists()) {
            return; // already migrated on a previous boot
        }
        plugin.getZoneDiscoveryService().purgeLegacyDynamicZoneDiscoveries();
        plugin.getAchievementManager().purgeLegacyDynamicZoneProgress();
        plugin.getLogger().info("RPGMood: migrated legacy dynamic-zone entries out of zonediscovery.yml and achievements.yml (Phase 2 zone clusters).");
    }
}
