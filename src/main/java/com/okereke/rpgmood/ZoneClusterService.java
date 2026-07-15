package com.okereke.rpgmood;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Organic, permanently-persisted zone clusters — the Phase 2 replacement for the old fixed
 * 256x256-block dynamic-zone grid. The first time a player reaches a chunk that isn't covered
 * by any curated zones.yml entry or existing cluster, a bounded flood-fill claims every
 * already-loaded neighboring chunk that shares its biome group, assigns it a permanent name
 * (reusing ZoneManager's adjective/noun generator), and persists it to dynamiczones.yml.
 * Clusters never grow or merge once created, and the flood-fill never forces chunk generation —
 * both deliberate scope simplifications, see the RPGMood Phase 2 design notes.
 */
public class ZoneClusterService {

    /**
     * Public view of a dynamic zone cluster — safe for other packages to read.
     * Every field is an accessor method on the corresponding private {@code Cluster} record field.
     */
    public record ClusterData(String id, String world, String biomeGroup, String displayName,
                              double centerX, double centerY, double centerZ,
                              int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ) {}

    private static final long SAVE_DELAY_TICKS = 100L; // ~5 seconds debounce, matches ZoneDiscoveryService

    private final RPGMoodPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private boolean saveScheduled = false;

    private int nextId;
    private final Map<String, String> chunkToClusterId = new HashMap<>();   // "world|cx|cz" -> id
    private final Map<String, Cluster> clustersById = new HashMap<>();      // id -> Cluster
    private final Set<String> usedNames = new HashSet<>();                  // never evicted — names are permanent
    private final Map<UUID, Long> lastClusterCreatedAt = new HashMap<>();   // per-player creation throttle

    public ZoneClusterService(RPGMoodPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "dynamiczones.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
        this.nextId = data.getInt("next_id", 1);
        loadClusters();
    }

    private void loadClusters() {
        ConfigurationSection clustersSection = data.getConfigurationSection("clusters");
        if (clustersSection == null) {
            return;
        }
        for (String idKey : clustersSection.getKeys(false)) {
            ConfigurationSection c = clustersSection.getConfigurationSection(idKey);
            if (c == null) {
                continue;
            }
            String biomeGroup = c.getString("biome_group", "PLAINS");
            Cluster cluster = new Cluster(
                    idKey,
                    c.getString("world", ""),
                    biomeGroup,
                    c.getString("flavor_group", biomeGroup),
                    c.getString("display_name", "Unknown Territory"),
                    c.getDouble("center_x"),
                    c.getDouble("center_y"),
                    c.getDouble("center_z"),
                    c.getInt("min_chunk_x"), c.getInt("max_chunk_x"),
                    c.getInt("min_chunk_z"), c.getInt("max_chunk_z")
            );
            clustersById.put(idKey, cluster);
            usedNames.add(cluster.displayName());
            for (String chunkStr : c.getStringList("chunks")) {
                String[] xz = chunkStr.split(",");
                if (xz.length != 2) {
                    continue;
                }
                chunkToClusterId.put(cluster.world() + "|" + xz[0] + "|" + xz[1], idKey);
            }
        }
    }

    /**
     * O(1) on the common "already-known chunk" path. Returns the resolved CLUSTER_ZONE key, or
     * null if the player's current chunk is unknown AND the per-player creation throttle hasn't
     * elapsed yet — callers must treat null as "not resolved this tick," not "no zone."
     */
    public String resolveClusterZone(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        String world = player.getWorld().getName();
        String chunkKey = world + "|" + chunk.getX() + "|" + chunk.getZ();

        String existingId = chunkToClusterId.get(chunkKey);
        if (existingId != null) {
            return ZoneManager.CLUSTER_ZONE_PREFIX + "|" + world + "|" + existingId;
        }

        UUID playerId = player.getUniqueId();
        long now = System.currentTimeMillis();
        long throttleMillis = plugin.getConfigManager().getConfigValues().getLong("dynamic_zones.creation_throttle_ms", 750L);
        Long last = lastClusterCreatedAt.get(playerId);
        if (last != null && now - last < throttleMillis) {
            return null;
        }

        String id = createCluster(player);
        lastClusterCreatedAt.put(playerId, now);
        return ZoneManager.CLUSTER_ZONE_PREFIX + "|" + world + "|" + id;
    }

    public String getDisplayName(String clusterZoneKey) {
        Cluster cluster = clusterFor(clusterZoneKey);
        return cluster != null ? cluster.displayName() : "Unknown Territory";
    }

    public String getBiomeGroup(String clusterZoneKey) {
        Cluster cluster = clusterFor(clusterZoneKey);
        return cluster != null ? cluster.biomeGroup() : "PLAINS";
    }

    /** The (possibly borrowed, via ZoneManager.resolveNamingBiomeGroup) pool this cluster's name/subtitle were drawn from — for display purposes use getBiomeGroup instead, which is always the real biome. */
    public String getFlavorGroup(String clusterZoneKey) {
        Cluster cluster = clusterFor(clusterZoneKey);
        return cluster != null ? cluster.flavorGroup() : "PLAINS";
    }

    /** Representative point for a cluster (its seed chunk's center, at surface height) — used by ZoneDiscoveryService for distance/danger lookups. Null if the cluster's world isn't loaded. */
    public Location getRepresentativeLocation(String clusterZoneKey) {
        Cluster cluster = clusterFor(clusterZoneKey);
        if (cluster == null) {
            return null;
        }
        World world = Bukkit.getWorld(cluster.world());
        if (world == null) {
            return null;
        }
        return new Location(world, cluster.centerX(), cluster.centerY(), cluster.centerZ());
    }

    /** Releases per-player tracking state; call on PlayerQuitEvent to avoid unbounded growth. */
    public void handlePlayerQuit(Player player) {
        lastClusterCreatedAt.remove(player.getUniqueId());
    }

    // ─────────────────────────────────────────────────────────────────
    //  Public read access for SquareMapHook (and other consumers)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Returns an immutable snapshot of every persisted cluster.
     * SquareMapHook iterates this list to register initial markers.
     */
    public java.util.Collection<ClusterData> getAllClusters() {
        java.util.ArrayList<ClusterData> result = new java.util.ArrayList<>(clustersById.size());
        for (Cluster c : clustersById.values()) {
            result.add(toClusterData(c));
        }
        return java.util.Collections.unmodifiableList(result);
    }

    /**
     * Returns a single cluster by its numeric id string, or null if unknown.
     */
    public ClusterData getCluster(String id) {
        Cluster c = clustersById.get(id);
        return c != null ? toClusterData(c) : null;
    }

    private static ClusterData toClusterData(Cluster c) {
        return new ClusterData(c.id(), c.world(), c.biomeGroup(), c.displayName(),
                c.centerX(), c.centerY(), c.centerZ(),
                c.minChunkX(), c.maxChunkX(), c.minChunkZ(), c.maxChunkZ());
    }

    private Cluster clusterFor(String clusterZoneKey) {
        String[] parts = clusterZoneKey.split("\\|");
        if (parts.length != 3 || !parts[0].equals(ZoneManager.CLUSTER_ZONE_PREFIX)) {
            return null;
        }
        return clustersById.get(parts[2]);
    }

    /** Bounded, synchronous, 4-connected flood-fill using only already-loaded chunks — never forces world generation. */
    private String createCluster(Player player) {
        World world = player.getWorld();
        String worldName = world.getName();
        Chunk seed = player.getLocation().getChunk();
        int seedCx = seed.getX();
        int seedCz = seed.getZ();
        // Sampled from the seed chunk's own center, never the player's exact block — guarantees
        // the seed chunk always matches its own membership test (seeding from the player's exact
        // biome could disagree with the chunk's center sample near an internal biome border,
        // permanently stranding that chunk from ever joining a cluster).
        String seedBiomeGroup = sampleChunkBiomeGroup(world, seedCx, seedCz);

        int maxChunks = plugin.getConfigManager().getConfigValues().getInt("dynamic_zones.max_cluster_chunks", 576);

        Set<String> visited = new HashSet<>();
        Deque<int[]> queue = new ArrayDeque<>();
        List<int[]> accepted = new ArrayList<>();
        visited.add(seedCx + "," + seedCz);
        queue.add(new int[]{seedCx, seedCz});

        int minCx = seedCx, maxCx = seedCx, minCz = seedCz, maxCz = seedCz;

        while (!queue.isEmpty() && accepted.size() < maxChunks) {
            int[] cur = queue.poll();
            int cx = cur[0];
            int cz = cur[1];
            if (!world.isChunkLoaded(cx, cz)) {
                continue;
            }
            if (!sampleChunkBiomeGroup(world, cx, cz).equals(seedBiomeGroup)) {
                continue;
            }

            accepted.add(cur);
            minCx = Math.min(minCx, cx);
            maxCx = Math.max(maxCx, cx);
            minCz = Math.min(minCz, cz);
            maxCz = Math.max(maxCz, cz);

            int[][] neighbors = {{cx + 1, cz}, {cx - 1, cz}, {cx, cz + 1}, {cx, cz - 1}};
            for (int[] n : neighbors) {
                if (visited.add(n[0] + "," + n[1])) {
                    queue.add(n);
                }
            }
        }
        // accepted is always non-empty: the seed chunk is tested against a biome group sampled
        // from itself, so it always passes its own membership check.

        String id = String.valueOf(nextId++);
        double centerX = seedCx * 16.0 + 8.0;
        double centerZ = seedCz * 16.0 + 8.0;
        double centerY = world.getHighestBlockYAt((int) centerX, (int) centerZ);

        // Which existing pool to borrow the name/subtitle from — the real biomeGroup above stays
        // the source of truth for flood-fill membership and for anything displaying "what biome
        // is this" (e.g. /rpgmood zones); this is purely a naming-flavor choice, computed once
        // here and persisted so it never needs re-sampling on every future title render.
        String seedRawBiome = world.getBiome((int) centerX, (int) centerY, (int) centerZ).name().toUpperCase(Locale.ROOT);
        String flavorGroup = ZoneManager.resolveNamingBiomeGroup(seedRawBiome,
                world.getTemperature((int) centerX, (int) centerY, (int) centerZ),
                world.getHumidity((int) centerX, (int) centerY, (int) centerZ));

        String seedKeyForNaming = ZoneManager.CLUSTER_ZONE_PREFIX + "|" + worldName + "|" + id;
        String displayName = plugin.getZoneManager().createUniqueZoneName(flavorGroup, seedKeyForNaming, usedNames::contains);
        usedNames.add(displayName);

        Cluster cluster = new Cluster(id, worldName, seedBiomeGroup, flavorGroup, displayName,
                centerX, centerY, centerZ, minCx, maxCx, minCz, maxCz);
        clustersById.put(id, cluster);

        List<String> chunkStrings = new ArrayList<>(accepted.size());
        for (int[] coord : accepted) {
            chunkToClusterId.put(worldName + "|" + coord[0] + "|" + coord[1], id);
            chunkStrings.add(coord[0] + "," + coord[1]);
        }

        persistCluster(id, cluster, chunkStrings);
        if (plugin.getSquareMapHook() != null) {
            plugin.getSquareMapHook().onClusterCreated(toClusterData(cluster));
        }
        return id;
    }

    /** Samples a chunk's biome group at its own center, at its own surface height — never a Y borrowed from another chunk, so hilly terrain near the seed doesn't corrupt membership. */
    private String sampleChunkBiomeGroup(World world, int chunkX, int chunkZ) {
        int x = chunkX * 16 + 8;
        int z = chunkZ * 16 + 8;
        int y = world.getHighestBlockYAt(x, z);
        return ZoneManager.normalizeBiomeGroup(world.getBiome(x, y, z).name().toUpperCase(Locale.ROOT));
    }

    private void persistCluster(String id, Cluster cluster, List<String> chunkStrings) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("world", cluster.world());
        entry.put("biome_group", cluster.biomeGroup());
        entry.put("flavor_group", cluster.flavorGroup());
        entry.put("display_name", cluster.displayName());
        entry.put("center_x", cluster.centerX());
        entry.put("center_y", cluster.centerY());
        entry.put("center_z", cluster.centerZ());
        entry.put("min_chunk_x", cluster.minChunkX());
        entry.put("max_chunk_x", cluster.maxChunkX());
        entry.put("min_chunk_z", cluster.minChunkZ());
        entry.put("max_chunk_z", cluster.maxChunkZ());
        entry.put("chunks", chunkStrings);

        data.set("clusters." + id, entry);
        data.set("next_id", nextId);
        scheduleSave();
    }

    private void scheduleSave() {
        if (saveScheduled) {
            return;
        }
        saveScheduled = true;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            saveScheduled = false;
            save();
        }, SAVE_DELAY_TICKS);
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save dynamiczones.yml: " + e.getMessage());
        }
    }

    private record Cluster(String id, String world, String biomeGroup, String flavorGroup, String displayName,
                            double centerX, double centerY, double centerZ,
                            int minChunkX, int maxChunkX, int minChunkZ, int maxChunkZ) {}
}
