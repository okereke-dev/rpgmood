package com.ricardo.rpgmood;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Generates narrative death messages in a short, clear format:
 *   &4⚰ &c{player} was slain by &4Zombie &7"But they wanted brains."
 *   &4⚰ &c{player} fell into the void &7"Nothing lasts forever."
 *   &4⚰ &c{player} was defeated by &4Steve &7"A worthy opponent."
 *
 * Colour scheme:
 *   &4 (dark red) — ⚰ icon + killer/cause emphasis
 *   &c (red)      — action verb + player name
 *   &7 (gray)     — humorous flavour quip (in quotes)
 *   &e (yellow)   — PvP-specific tone
 */
public class DeathMessageListener implements Listener {

    private static final String PREFIX = "&4\u2694 ";
    private static final String FLAVOUR_OPEN = " &7\"";
    private static final String FLAVOUR_CLOSE = "\"";

    private final RPGMoodPlugin plugin;
    private final Random random = new Random();
    private final Map<UUID, String> lastTemplates = new HashMap<>();

    private static final Map<String, String> BIOME_ALIAS = buildBiomeAlias();

    public DeathMessageListener(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player == null) return;

        plugin.getPlayerStatsService().recordDeath(player);
        plugin.getAchievementManager().resetDeathStreak(player);

        String causeKey = detectCause(event);
        String biomeKey = player.getLocation().getBlock().getBiome().name().toLowerCase(Locale.ROOT);
        String locationName = resolveLocationName(player);
        String killerKey = detectKiller(event);
        Entity killerEntity = event.getEntity().getKiller();

        String message = buildDeathMessage(player, causeKey, biomeKey, killerKey, killerEntity, locationName);

        if (message != null && !message.isBlank()) {
            // Fire API event
            com.ricardo.rpgmood.api.PlayerDeathMessageEvent apiEvent =
                    new com.ricardo.rpgmood.api.PlayerDeathMessageEvent(player, message, causeKey, biomeKey, killerKey);
            plugin.getServer().getPluginManager().callEvent(apiEvent);

            if (!apiEvent.isCancelled() && apiEvent.getMessage() != null) {
                String translated = ChatColor.translateAlternateColorCodes('&', apiEvent.getMessage());
                event.setDeathMessage(translated);
                plugin.getPlayerJournalService().addEntry(player, translated);
                return;
            }
        }

        // Fallback
        event.setDeathMessage("");
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastTemplates.remove(event.getPlayer().getUniqueId());
    }

    /** Builds the full death message from components. */
    private String buildDeathMessage(Player player, String causeKey, String biomeKey,
                                     String killerKey, Entity killerEntity, String locationName) {
        var root = plugin.getConfig().getConfigurationSection("death_messages");
        if (root == null) return null;

        boolean isPvP = "player".equals(killerKey) && killerEntity instanceof Player;
        boolean hasKiller = !"none".equals(killerKey) && !"unknown".equals(killerKey);

        String action;
        String killerDisplay;
        String flavour;

        if (isPvP) {
            // PvP death
            action = pickRandom(root, "pvp_actions");
            if (action == null) action = "was defeated by";
            killerDisplay = ((Player) killerEntity).getName();
            flavour = pickRandom(root, "flavours.player");
        } else if (hasKiller) {
            // Mob kill
            action = pickAction(root, "entity");
            killerDisplay = prettify(killerKey);
            flavour = pickRandom(root, "flavours." + killerKey);
        } else {
            // Environmental death
            action = pickAction(root, causeKey);
            if (action == null) action = pickAction(root, "unknown");
            killerDisplay = null;
            flavour = pickRandom(root, "environmental_flavours." + causeKey);
        }

        if (action == null) action = "met an untimely end";
        if (flavour == null) flavour = pickRandom(root, "flavours.default");
        if (flavour == null) flavour = "Game Over.";

        UUID id = player.getUniqueId();
        StringBuilder sb = new StringBuilder();
        sb.append(PREFIX).append("&c").append(player.getName()).append(" ").append(action);

        if (killerDisplay != null) {
            sb.append(" &4").append(killerDisplay);
        }

        sb.append(FLAVOUR_OPEN).append(flavour).append(FLAVOUR_CLOSE);

        String result = sb.toString();

        // Avoid repeating the exact same line
        if (result.equals(lastTemplates.get(id))) {
            return buildDeathMessage(player, causeKey, biomeKey, killerKey, killerEntity, locationName);
        }
        lastTemplates.put(id, result);

        return result;
    }

    /** Picks a random action verb for the given cause key from death_messages.actions. */
    private String pickAction(ConfigurationSection root, String causeKey) {
        List<String> actions = root.getStringList("actions." + causeKey);
        if (actions == null || actions.isEmpty()) return null;
        return actions.get(random.nextInt(actions.size()));
    }

    /** Picks a random string from a list path. */
    private String pickRandom(ConfigurationSection root, String path) {
        List<String> items = root.getStringList(path);
        if (items == null || items.isEmpty()) return null;
        return items.get(random.nextInt(items.size()));
    }

    private String detectCause(PlayerDeathEvent event) {
        if (event.getEntity().getLastDamageCause() == null) return "unknown";
        String cause = event.getEntity().getLastDamageCause().getCause().name().toLowerCase(Locale.ROOT);

        // ENTITY_EXPLOSION (creeper, ghast fireball) — map to "explosion" not "entity"
        if ("entity_explosion".equals(cause)) return "explosion";
        // ENTITY_ATTACK, ENTITY_SWEEP_ATTACK, PROJECTILE — these have a real killer
        if (cause.contains("entity") || cause.contains("projectile")) return "entity";
        if (cause.contains("fire") || cause.contains("lava") || cause.contains("burn")) return "fire";
        if (cause.contains("fall")) return "fall";
        if (cause.contains("void")) return "void";
        if (cause.contains("explosion") || cause.contains("block_explosion")) return "explosion";
        if (cause.contains("drowning")) return "drowning";
        return "unknown";
    }

    /**
     * Detects the entity that killed the player.
     * First tries {@code getKiller()} (works for direct melee/ranged kills),
     * then falls back to checking the damager from the last damage cause
     * (catches creepers, ghast fireballs, etc.).
     */
    private String detectKiller(PlayerDeathEvent event) {
        Player player = event.getEntity();

        // 1. Direct killer (melee, arrow, etc.)
        Entity killer = player.getKiller();
        if (killer != null) return killer.getType().name().toLowerCase(Locale.ROOT);

        // 2. Check last damage cause for an entity damager (creeper, ghast, etc.)
        if (player.getLastDamageCause() instanceof EntityDamageByEntityEvent damageEvent) {
            Entity damager = damageEvent.getDamager();
            if (damager != null) {
                return damager.getType().name().toLowerCase(Locale.ROOT);
            }
        }

        return "none";
    }

    private String resolveLocationName(Player player) {
        String biomeKey = player.getLocation().getBlock().getBiome().name().toLowerCase(Locale.ROOT);
        String normalized = BIOME_ALIAS.getOrDefault(biomeKey, biomeKey);
        var root = plugin.getConfig().getConfigurationSection("death_messages");
        if (root == null) return biomeKey.replace('_', ' ');

        var namesRoot = root.getConfigurationSection("location_names");
        if (namesRoot == null) return biomeKey.replace('_', ' ');

        List<String> names = namesRoot.getStringList(normalized);
        if (!names.isEmpty()) {
            return buildLocationName(root, normalized, names);
        }

        // Try fuzzy matching
        for (String key : namesRoot.getKeys(false)) {
            String k = key.toLowerCase(Locale.ROOT);
            if (normalized.contains(k) || k.contains(normalized)) {
                List<String> alt = namesRoot.getStringList(key);
                if (!alt.isEmpty()) return buildLocationName(root, key, alt);
            }
        }

        return biomeKey.replace('_', ' ');
    }

    private String buildLocationName(ConfigurationSection root, String normalized, List<String> names) {
        String base = names.get(random.nextInt(names.size()));
        String template = pickRandom(root, "location_name_templates");
        String descriptor = pickDescriptor(root, normalized);

        if (template == null || template.isBlank() || template.equals("{base}") || descriptor == null) {
            return base;
        }
        return template.replace("{base}", base).replace("{descriptor}", descriptor);
    }

    private String pickDescriptor(ConfigurationSection root, String normalized) {
        List<String> descriptors = root.getStringList("location_name_descriptors." + normalized);
        if (descriptors == null || descriptors.isEmpty()) {
            descriptors = root.getStringList("location_name_descriptors.default");
        }
        if (descriptors == null || descriptors.isEmpty()) return null;
        return descriptors.get(random.nextInt(descriptors.size()));
    }

    private String prettify(String enumName) {
        String[] parts = enumName.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private static Map<String, String> buildBiomeAlias() {
        return Map.ofEntries(
                Map.entry("sunflower_plains", "plains"),
                Map.entry("meadow", "plains"),
                Map.entry("flower_forest", "plains"),
                Map.entry("forest", "forest"),
                Map.entry("birch_forest", "forest"),
                Map.entry("old_growth_birch_forest", "forest"),
                Map.entry("old_growth_pine_taiga", "snowy_taiga"),
                Map.entry("old_growth_spruce_taiga", "snowy_taiga"),
                Map.entry("giant_tree_taiga", "taiga"),
                Map.entry("giant_spruce_taiga", "taiga"),
                Map.entry("ice_spikes", "snowy_taiga"),
                Map.entry("snowy_plains", "snowy_taiga"),
                Map.entry("snowy_mountains", "mountains"),
                Map.entry("frozen_peaks", "mountains"),
                Map.entry("jagged_peaks", "mountains"),
                Map.entry("grove", "dark_forest"),
                Map.entry("bamboo_jungle", "jungle"),
                Map.entry("mangrove_swamp", "swamp"),
                Map.entry("crimson_forest", "crimson_forest"),
                Map.entry("warped_forest", "warped_forest"),
                Map.entry("basalt_deltas", "basalt_deltas"),
                Map.entry("soul_sand_valley", "soul_sand_valley"),
                Map.entry("nether_wastes", "nether_wastes"),
                Map.entry("beach", "beach"),
                Map.entry("stone_shore", "beach"),
                Map.entry("river", "river"),
                Map.entry("frozen_river", "river"),
                Map.entry("ocean", "ocean"),
                Map.entry("lukewarm_ocean", "ocean"),
                Map.entry("warm_ocean", "ocean"),
                Map.entry("deep_ocean", "ocean"),
                Map.entry("cold_ocean", "ocean"),
                Map.entry("frozen_ocean", "ocean"),
                Map.entry("mushroom_fields", "mushroom"),
                Map.entry("the_end", "end"),
                Map.entry("end_midlands", "end"),
                Map.entry("end_highlands", "end"),
                Map.entry("end_barrens", "end"),
                Map.entry("small_end_islands", "end")
        );
    }
}
