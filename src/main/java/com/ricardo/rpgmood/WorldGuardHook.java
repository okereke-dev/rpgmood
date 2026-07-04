package com.ricardo.rpgmood;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;

/**
 * All direct references to WorldGuard/WorldEdit classes live in this file only. If WorldGuard
 * is not installed, this class is never loaded/verified by the JVM as long as callers guard
 * every entry point with a presence check first (see RPGMoodPlugin.isWorldGuardActive()).
 */
public final class WorldGuardHook {

    private WorldGuardHook() {}

    public static boolean isInsideRegion(Location location, String regionId) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet regions = query.getApplicableRegions(BukkitAdapter.adapt(location));
        for (ProtectedRegion region : regions) {
            if (region.getId().equalsIgnoreCase(regionId)) {
                return true;
            }
        }
        return false;
    }
}
