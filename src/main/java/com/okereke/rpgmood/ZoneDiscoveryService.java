package com.okereke.rpgmood;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Persists which zones each player has discovered (first-seen date, favorite flag) for /rpgmood zones. */
public class ZoneDiscoveryService {

    private static final long SAVE_DELAY_TICKS = 100L; // ~5 seconds debounce

    private final RPGMoodPlugin plugin;
    private final File file;
    private final YamlConfiguration data;
    private boolean saveScheduled = false;

    public ZoneDiscoveryService(RPGMoodPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "zonediscovery.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    /** Records first discovery of a zone for this player, if not already recorded. */
    public void recordDiscovery(Player player, String zoneKey, String displayName) {
        String path = "players." + player.getUniqueId();
        List<Map<?, ?>> entries = data.getMapList(path);
        for (Map<?, ?> entry : entries) {
            if (zoneKey.equals(entry.get("key"))) return;
        }

        List<Map<String, Object>> updated = copyEntries(entries);
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("key", zoneKey);
        entry.put("display", displayName);
        entry.put("first_seen", System.currentTimeMillis());
        entry.put("favorite", false);
        updated.add(entry);

        data.set(path, updated);
        scheduleSave();
    }

    public List<DiscoveredZone> getDiscoveries(Player player) {
        List<Map<?, ?>> entries = data.getMapList("players." + player.getUniqueId());
        List<DiscoveredZone> result = new ArrayList<>();
        for (Map<?, ?> entry : entries) {
            result.add(new DiscoveredZone(
                    String.valueOf(entry.get("key")),
                    String.valueOf(entry.get("display")),
                    entry.get("first_seen") instanceof Number n ? n.longValue() : 0L,
                    Boolean.TRUE.equals(entry.get("favorite"))
            ));
        }
        return result;
    }

    /** Toggles the favorite flag for a discovered zone by display name (case-insensitive). Returns the new state, or null if the player never discovered a zone with that name. */
    public Boolean toggleFavorite(Player player, String displayName) {
        String path = "players." + player.getUniqueId();
        List<Map<?, ?>> entries = data.getMapList(path);
        List<Map<String, Object>> updated = copyEntries(entries);

        Boolean result = null;
        for (Map<String, Object> entry : updated) {
            if (displayName.equalsIgnoreCase(String.valueOf(entry.get("display")))) {
                boolean newValue = !Boolean.TRUE.equals(entry.get("favorite"));
                entry.put("favorite", newValue);
                result = newValue;
            }
        }
        if (result != null) {
            data.set(path, updated);
            scheduleSave();
        }
        return result;
    }

    /** Biome group for a discovered zone key — parsed from the key for dynamic zones, read from zones.yml for curated BIOME-type ones, "—" otherwise (e.g. WORLDGUARD zones). */
    public String biomeOf(String zoneKey) {
        if (zoneKey.startsWith("DYNAMIC_ZONE")) {
            String[] parts = zoneKey.split("\\|");
            return parts.length >= 2 ? parts[1] : "?";
        }
        var section = plugin.getConfigManager().getZones().getConfigurationSection("zones." + zoneKey);
        if (section != null && "BIOME".equalsIgnoreCase(section.getString("type", "BIOME"))) {
            return section.getString("id", "—");
        }
        return "—";
    }

    /**
     * Current danger level for a discovered zone, or null if it can't be computed. Only works
     * for dynamic zones (their key embeds an approximate region center); curated BIOME/
     * WORLDGUARD zones in zones.yml have no single fixed location to measure from.
     */
    public Integer dangerLevelOf(Player player, String zoneKey) {
        Location location = approximateLocation(player, zoneKey);
        if (location == null) return null;
        int baseLevel = plugin.getMobScalingService().getBaseLevel(EntityType.ZOMBIE);
        return plugin.getMobScalingService().calculateLevelAt(location, baseLevel);
    }

    /** Distance to the nearest zone the player has already discovered whose location can be reconstructed (dynamic zones only), or null if none qualify. */
    public Double distanceToNearestDiscoveredZone(Player player) {
        double best = Double.MAX_VALUE;
        for (DiscoveredZone zone : getDiscoveries(player)) {
            Location location = approximateLocation(player, zone.key());
            if (location == null) continue;
            double distance = player.getLocation().distance(location);
            if (distance > 8.0 && distance < best) { // skip "you're standing in it right now"
                best = distance;
            }
        }
        return best == Double.MAX_VALUE ? null : best;
    }

    /** Parses "DYNAMIC_ZONE|biome|world|regionX|regionZ" into an approximate region-center location. Null for curated zones (no single fixed point) or unloaded worlds. */
    private Location approximateLocation(Player player, String zoneKey) {
        String[] parts = zoneKey.split("\\|");
        if (parts.length != 5 || !parts[0].equals("DYNAMIC_ZONE")) return null;
        World world = Bukkit.getWorld(parts[2]);
        if (world == null) return null;
        try {
            int regionX = Integer.parseInt(parts[3]);
            int regionZ = Integer.parseInt(parts[4]);
            double centerX = regionX * 256.0 + 128.0;
            double centerZ = regionZ * 256.0 + 128.0;
            return new Location(world, centerX, player.getLocation().getY(), centerZ);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> copyEntries(List<Map<?, ?>> entries) {
        List<Map<String, Object>> copy = new ArrayList<>();
        for (Map<?, ?> entry : entries) {
            copy.add(new LinkedHashMap<>((Map<String, Object>) entry));
        }
        return copy;
    }

    private void scheduleSave() {
        if (saveScheduled) return;
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
            plugin.getLogger().warning("Could not save zonediscovery.yml: " + e.getMessage());
        }
    }

    public record DiscoveredZone(String key, String display, long firstSeenMillis, boolean favorite) {}
}
