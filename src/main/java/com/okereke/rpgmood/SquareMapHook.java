package com.okereke.rpgmood;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * All direct references to SquareMap classes live in this file only. Draws each zone cluster as
 * a borderless, semi-transparent rectangle colored by local danger, with the zone name shown on
 * hover — SquareMap's marker API has no "always visible floating label" type, only hover/click
 * tooltips (verified against the real xyz.jpenilla:squaremap-api source, not assumed). Since
 * clusters are immutable once created (Phase 2 design), there's no periodic refresh task —
 * markers are registered once at startup for existing clusters, and once more, incrementally,
 * whenever a new cluster is created.
 */
public final class SquareMapHook {

    private static final Key LAYER_KEY = Key.of("rpgmood_zones");

    private final RPGMoodPlugin plugin;
    private final Map<WorldIdentifier, SimpleLayerProvider> providers = new HashMap<>();
    private Squaremap squaremap;
    private boolean active = false;

    public SquareMapHook(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    /** Called once from onEnable(), after zoneClusterService. No-op if SquareMap isn't installed. */
    public void enable() {
        if (Bukkit.getPluginManager().getPlugin("squaremap") == null) {
            return;
        }
        try {
            this.squaremap = SquaremapProvider.get();
        } catch (IllegalStateException e) {
            plugin.getLogger().warning("SquareMap is installed but its API isn't ready: " + e.getMessage());
            return;
        }

        for (World world : Bukkit.getWorlds()) {
            registerWorldLayer(world);
        }
        for (ZoneClusterService.ClusterData cluster : plugin.getZoneClusterService().getAllClusters()) {
            addMarker(cluster);
        }
        active = true;
        plugin.getLogger().info("SquareMap integration enabled — " + plugin.getZoneClusterService().getAllClusters().size() + " zones mapped.");
    }

    /** Called from onDisable() — unregisters RPGMood's layer from every world that had one. */
    public void disable() {
        if (!active) {
            return;
        }
        providers.forEach((worldId, provider) ->
                squaremap.getWorldIfEnabled(worldId).ifPresent(mapWorld -> {
                    if (mapWorld.layerRegistry().hasEntry(LAYER_KEY)) {
                        mapWorld.layerRegistry().unregister(LAYER_KEY);
                    }
                }));
        providers.clear();
        active = false;
    }

    /** Clears and re-adds every marker from the current cluster data — cheap self-healing hook for /rpgmood reload, since clusters are read straight from ZoneClusterService each time. */
    public void reload() {
        if (!active) {
            return;
        }
        providers.values().forEach(SimpleLayerProvider::clearMarkers);
        for (ZoneClusterService.ClusterData cluster : plugin.getZoneClusterService().getAllClusters()) {
            addMarker(cluster);
        }
    }

    /** Called once per cluster, right when it's created — clusters never change afterward, so this is the only "add" a given cluster ever needs. */
    public void onClusterCreated(ZoneClusterService.ClusterData cluster) {
        if (active) {
            addMarker(cluster);
        }
    }

    private void registerWorldLayer(World world) {
        squaremap.getWorldIfEnabled(BukkitAdapter.worldIdentifier(world)).ifPresent(mapWorld ->
                providers.computeIfAbsent(mapWorld.identifier(), id -> {
                    SimpleLayerProvider provider = SimpleLayerProvider.builder("RPGMood Zones")
                            .layerPriority(plugin.getConfig().getInt("square_map.layer_priority", 3))
                            .build();
                    mapWorld.layerRegistry().register(LAYER_KEY, provider);
                    return provider;
                }));
    }

    private void addMarker(ZoneClusterService.ClusterData cluster) {
        World world = Bukkit.getWorld(cluster.world());
        if (world == null) {
            return;
        }
        registerWorldLayer(world); // no-op if this world's layer already exists
        SimpleLayerProvider provider = providers.get(BukkitAdapter.worldIdentifier(world));
        if (provider == null) {
            return;
        }

        // +1 on the max corner to cover the full last chunk, not just up to its near edge —
        // same detail the official SquareMap WorldGuard addon uses for cuboid regions.
        Marker marker = Marker.rectangle(
                Point.of(cluster.minChunkX() * 16.0, cluster.minChunkZ() * 16.0),
                Point.of((cluster.maxChunkX() + 1) * 16.0, (cluster.maxChunkZ() + 1) * 16.0));

        int dangerLevel = plugin.getMobScalingService().calculateLevelAt(
                new Location(world, cluster.centerX(), cluster.centerY(), cluster.centerZ()),
                plugin.getMobScalingService().getBaseLevel(EntityType.ZOMBIE));

        marker.markerOptions(MarkerOptions.builder()
                .stroke(false)
                .fill(true)
                .fillColor(dangerColor(dangerLevel))
                .fillOpacity(plugin.getConfig().getDouble("square_map.zone_fill_opacity", 0.18))
                .hoverTooltip(cluster.displayName())
                .build());

        provider.addMarker(Key.of("rpgmood_cluster_" + cluster.id()), marker);
    }

    /** Same 5 danger tiers and colors as ZoneManager.getZoneDangerColorCode()/ZoneScoreboardService, in RGB. */
    private static Color dangerColor(int level) {
        if (level <= 3) return new Color(170, 170, 170);   // Common
        if (level <= 9) return new Color(255, 255, 85);    // Uncommon
        if (level <= 17) return new Color(170, 0, 170);    // Rare
        if (level <= 27) return new Color(85, 255, 85);    // Hero
        return new Color(255, 170, 0);                     // Legendary
    }
}
