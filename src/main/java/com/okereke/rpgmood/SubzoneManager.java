package com.okereke.rpgmood;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.GeneratedStructure;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Detects cave-biome and vanilla-structure "subzones" a player is standing inside and shows a
 * Title/Subtitle for them — a lighter-weight, side-effect-free sibling to {@link ZoneManager}'s
 * zone system. Deliberately does not touch discovery/XP/achievements/stats: a subzone is a
 * flavor detail inside a real zone, not a zone in its own right (see subzones.yml's header
 * comment and the RPGMood subzone design plan for why).
 */
public class SubzoneManager {

    /** Raw Structure-registry keys (as returned by Chunk#getStructures()) grouped into subzone ids. */
    private static final Map<String, String> STRUCTURE_GROUP = Map.ofEntries(
            Map.entry("village_plains", "VILLAGE"),
            Map.entry("village_desert", "VILLAGE"),
            Map.entry("village_savanna", "VILLAGE"),
            Map.entry("village_snowy", "VILLAGE"),
            Map.entry("village_taiga", "VILLAGE"),
            Map.entry("pillager_outpost", "PILLAGER_OUTPOST"),
            Map.entry("desert_pyramid", "DESERT_PYRAMID"),
            Map.entry("jungle_pyramid", "JUNGLE_TEMPLE"),
            Map.entry("mansion", "WOODLAND_MANSION"),
            Map.entry("monument", "OCEAN_MONUMENT"),
            Map.entry("fortress", "NETHER_FORTRESS"),
            Map.entry("stronghold", "STRONGHOLD")
    );

    /** Breaks ties when multiple structure subzones are in range at once — rarer/more notable wins. */
    private static final List<String> STRUCTURE_PRIORITY = List.of(
            "STRONGHOLD", "WOODLAND_MANSION", "OCEAN_MONUMENT", "NETHER_FORTRESS",
            "PILLAGER_OUTPOST", "JUNGLE_TEMPLE", "DESERT_PYRAMID", "VILLAGE"
    );

    private final RPGMoodPlugin plugin;
    private final Map<UUID, Long> lastCheckAt = new HashMap<>();
    private final Map<UUID, Subzone> pendingSubzone = new HashMap<>();
    private final Map<UUID, Subzone> activeSubzone = new HashMap<>();
    private final Map<UUID, Map<Subzone, Long>> lastTitleShownAt = new HashMap<>();

    public SubzoneManager(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    public void handlePlayerMove(Player player) {
        if (!plugin.getConfigManager().getSubzones().getBoolean("subzones.enabled", true)) {
            return;
        }

        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long throttleMillis = plugin.getConfigManager().getSubzones().getLong("subzones.check_throttle_ms", 500L);
        Long last = lastCheckAt.get(id);
        if (last != null && now - last < throttleMillis) {
            return;
        }
        lastCheckAt.put(id, now);

        Subzone detected = detectStructureSubzone(player);
        if (detected == null) {
            detected = detectCaveSubzone(player);
        }

        if (detected == null) {
            pendingSubzone.remove(id);
            activeSubzone.remove(id);
            return;
        }

        if (detected.equals(activeSubzone.get(id))) {
            return;
        }

        if (detected.equals(pendingSubzone.get(id))) {
            // Confirmed on two consecutive throttled reads — filters border flicker the same
            // way ZoneManager's dwell time does for full zones, just without a scheduled task.
            activeSubzone.put(id, detected);
            pendingSubzone.remove(id);
            maybeShowTitle(player, detected, now);
        } else {
            pendingSubzone.put(id, detected);
        }
    }

    /** Releases per-player tracking state; call on PlayerQuitEvent to avoid unbounded growth. */
    public void handlePlayerQuit(Player player) {
        UUID id = player.getUniqueId();
        lastCheckAt.remove(id);
        pendingSubzone.remove(id);
        activeSubzone.remove(id);
        lastTitleShownAt.remove(id);
    }

    /**
     * Checks the player's chunk and its neighbors (radius from subzones.yml) for already-generated
     * vanilla structures via Chunk#getStructures() — a read of data the chunk already carries, not
     * a radius search like MobScalingService's locateNearestStructure-based mob bonus.
     */
    private Subzone detectStructureSubzone(Player player) {
        ConfigurationSection section = plugin.getConfigManager().getSubzones().getConfigurationSection("subzones.structures");
        if (section == null) {
            return null;
        }

        int radius = section.getInt("chunk_radius", 1);
        World world = player.getWorld();
        Chunk center = player.getLocation().getChunk();
        int centerX = center.getX();
        int centerZ = center.getZ();

        Set<String> found = new HashSet<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = centerX + dx;
                int cz = centerZ + dz;
                if (!world.isChunkLoaded(cx, cz)) {
                    continue;
                }
                for (GeneratedStructure structure : world.getChunkAt(cx, cz).getStructures()) {
                    String group = STRUCTURE_GROUP.get(structure.getStructure().getKey().getKey());
                    if (group != null) {
                        found.add(group);
                    }
                }
            }
        }

        if (found.isEmpty()) {
            return null;
        }
        for (String candidate : STRUCTURE_PRIORITY) {
            if (found.contains(candidate)) {
                return new Subzone("structures", candidate);
            }
        }
        return null;
    }

    /** Real 3D cave biomes first (Lush Caves / Dripstone Caves / Deep Dark), then a Y-band fallback for plain stone. */
    private Subzone detectCaveSubzone(Player player) {
        Biome biome = player.getLocation().getBlock().getBiome();
        if (biome == Biome.LUSH_CAVES) {
            return new Subzone("caves", "LUSH_CAVES");
        }
        if (biome == Biome.DRIPSTONE_CAVES) {
            return new Subzone("caves", "DRIPSTONE_CAVES");
        }
        if (biome == Biome.DEEP_DARK) {
            return new Subzone("caves", "DEEP_DARK");
        }

        if (!isUnderground(player)) {
            return null;
        }

        ConfigurationSection section = plugin.getConfigManager().getSubzones().getConfigurationSection("subzones.caves");
        int deepThresholdY = section != null ? section.getInt("deep_threshold_y", 0) : 0;
        String id = player.getLocation().getBlockY() < deepThresholdY ? "DEEP_CAVES" : "SHALLOW_CAVES";
        return new Subzone("caves", id);
    }

    /** Cheap heightmap check — well below the surface at this x/z, not just standing in a valley or ravine. */
    private boolean isUnderground(Player player) {
        ConfigurationSection section = plugin.getConfigManager().getSubzones().getConfigurationSection("subzones.caves");
        int margin = section != null ? section.getInt("underground_surface_margin", 8) : 8;
        World world = player.getWorld();
        int x = player.getLocation().getBlockX();
        int z = player.getLocation().getBlockZ();
        return player.getLocation().getBlockY() < world.getHighestBlockYAt(x, z) - margin;
    }

    private void maybeShowTitle(Player player, Subzone subzone, long now) {
        UUID id = player.getUniqueId();
        if (!plugin.getConfigManager().getConfigValues().getBoolean("player_effects." + id, true)) {
            return;
        }
        if (!plugin.getConfigManager().getConfigValues().getBoolean("player_titles." + id, true)) {
            return;
        }

        long suppressMillis = plugin.getConfigManager().getSubzones().getLong("subzones.title_suppress_minutes", 3L) * 60_000L;
        Map<Subzone, Long> shown = lastTitleShownAt.computeIfAbsent(id, k -> new HashMap<>());
        Long lastShown = shown.get(subzone);
        if (lastShown != null && now - lastShown < suppressMillis) {
            return;
        }
        shown.put(subzone, now);

        renderSubzoneTitle(player, subzone);
    }

    /** Big Title line = subzone name (colored by local danger, same tier palette as zones); small Subtitle = the main zone as context. */
    private void renderSubzoneTitle(Player player, Subzone subzone) {
        ConfigurationSection section = plugin.getConfigManager().getSubzones()
                .getConfigurationSection("subzones." + subzone.category() + ".entries." + subzone.id());
        String titleText = section != null ? section.getString("title", subzone.id()) : subzone.id();

        String strippedTitle = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', titleText));
        String legacyTitle = ChatColor.translateAlternateColorCodes('&', plugin.getZoneManager().getZoneDangerColorCode(player) + strippedTitle);
        String legacySubtitle = ChatColor.translateAlternateColorCodes('&', "&7" + plugin.getZoneManager().getCurrentZoneDisplayName(player));

        player.sendTitle(legacyTitle, legacySubtitle, 10, 40, 10);
    }

    private record Subzone(String category, String id) {}
}
