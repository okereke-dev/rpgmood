package com.ricardo.rpgmood;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class DeathMessageListener implements Listener {

    /** Chance (0-99) that a closing "modifiers" flourish is appended to the chosen message. */
    private static final int MODIFIER_CHANCE = 45;

    private final RPGMoodPlugin plugin;
    private final Random random = new Random();
    private final Map<UUID, String> lastTemplates = new HashMap<>();
    /** Maps Minecraft biome names to their death-message category key, aligned with ZoneManager.BIOME_GROUP. */
    private static final Map<String, String> BIOME_ALIAS = Map.ofEntries(
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
            Map.entry("warm_beach", "beach"),
            Map.entry("snowy_beach", "beach"),
            Map.entry("river", "river"),
            Map.entry("frozen_river", "river"),
            Map.entry("ocean", "ocean"),
            Map.entry("lukewarm_ocean", "ocean"),
            Map.entry("warm_ocean", "ocean"),
            Map.entry("deep_ocean", "ocean"),
            Map.entry("cold_ocean", "ocean"),
            Map.entry("frozen_ocean", "ocean"),
            Map.entry("mushroom_fields", "mushroom"),
            Map.entry("mushroom_field_shore", "mushroom"),
            Map.entry("the_end", "end"),
            Map.entry("end_midlands", "end"),
            Map.entry("end_highlands", "end"),
            Map.entry("end_barrens", "end"),
            Map.entry("small_end_islands", "end")
    );

    public DeathMessageListener(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (player == null) {
            return;
        }

        plugin.getPlayerStatsService().recordDeath(player);

        String causeKey = detectCause(event);
        String biomeKey = player.getLocation().getBlock().getBiome().name().toLowerCase(Locale.ROOT);
        String locationName = resolveLocationName(player);
        String killerKey = detectKiller(event);
        Entity killerEntity = event.getEntity().getKiller();
        boolean armed = hasWeapon(player);

        String selectedMessage = selectMessage(causeKey, biomeKey, killerKey, killerEntity, player, locationName, armed);
        if (selectedMessage != null && !selectedMessage.isBlank()) {
            String translated = ChatColor.translateAlternateColorCodes('&', selectedMessage);
            event.setDeathMessage(translated);
            plugin.getPlayerJournalService().addEntry(player, translated);
        } else {
            event.setDeathMessage("");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastTemplates.remove(event.getPlayer().getUniqueId());
    }

    private String detectCause(PlayerDeathEvent event) {
        if (event.getEntity().getLastDamageCause() == null) {
            return "unknown";
        }

        String cause = event.getEntity().getLastDamageCause().getCause().name().toLowerCase(Locale.ROOT);
        if (cause.contains("entity")) {
            return "entity";
        }
        if (cause.contains("fire") || cause.contains("lava") || cause.contains("burn")) {
            return "fire";
        }
        if (cause.contains("fall")) {
            return "fall";
        }
        if (cause.contains("void")) {
            return "void";
        }
        if (cause.contains("explosion")) {
            return "explosion";
        }
        if (cause.contains("drowning")) {
            return "drowning";
        }
        return cause;
    }

    private String detectKiller(PlayerDeathEvent event) {
        Entity killer = event.getEntity().getKiller();
        if (killer == null) {
            return "none";
        }
        return killer.getType().name().toLowerCase(Locale.ROOT);
    }

    private String resolveLocationName(Player player) {
        String biomeKey = player.getLocation().getBlock().getBiome().name().toLowerCase(Locale.ROOT);
        String normalized = BIOME_ALIAS.getOrDefault(biomeKey, biomeKey);
        var deathRoot = plugin.getConfig().getConfigurationSection("death_messages");
        if (deathRoot == null) {
            return biomeKey.replace('_', ' ');
        }

        var root = deathRoot.getConfigurationSection("location_names");
        if (root == null) {
            return biomeKey.replace('_', ' ');
        }

        List<String> names = root.getStringList(normalized);
        if (!names.isEmpty()) {
            return buildLocationName(deathRoot, normalized, names);
        }

        if (!normalized.equals(biomeKey)) {
            names = root.getStringList(biomeKey);
            if (!names.isEmpty()) {
                return buildLocationName(deathRoot, biomeKey, names);
            }
        }

        // Try flexible matching against configured keys (allows fuzzy matches like snowy_plains -> snowy_taiga)
        for (String key : root.getKeys(false)) {
            String k = key.toLowerCase(Locale.ROOT);
            String cleanKey = k.replaceAll("[ _]", "");
            String cleanBiome = normalized.replaceAll("[ _]", "");
            if (cleanBiome.contains(cleanKey) || cleanKey.contains(cleanBiome) || normalized.contains(k) || k.contains(normalized)) {
                List<String> alt = root.getStringList(key);
                if (!alt.isEmpty()) {
                    return buildLocationName(deathRoot, key, alt);
                }
            }
        }

        // Fallback: pretty biome name
        return biomeKey.replace('_', ' ');
    }

    private String buildLocationName(org.bukkit.configuration.ConfigurationSection deathRoot, String normalized, List<String> names) {
        String base = names.get(random.nextInt(names.size()));
        String template = pickRandom(deathRoot, "location_name_templates");
        String descriptor = pickDescriptor(deathRoot, normalized);

        if (template == null || template.isBlank() || template.equals("{base}") || descriptor == null || descriptor.isBlank()) {
            return base;
        }
        return formatLocationName(template, base, descriptor);
    }

    private String pickDescriptor(org.bukkit.configuration.ConfigurationSection root, String normalized) {
        List<String> descriptors = root.getStringList("location_name_descriptors." + normalized);
        if (descriptors == null || descriptors.isEmpty()) {
            descriptors = root.getStringList("location_name_descriptors.default");
        }
        if (descriptors == null || descriptors.isEmpty()) {
            return null;
        }
        return descriptors.get(random.nextInt(descriptors.size()));
    }

    private String formatLocationName(String template, String base, String descriptor) {
        return template.replace("{base}", base).replace("{descriptor}", descriptor);
    }

    private boolean hasWeapon(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) {
                continue;
            }
            String type = item.getType().name().toLowerCase(Locale.ROOT);
            if (type.contains("sword") || type.contains("axe") || type.contains("bow") || type.contains("trident") || type.contains("pickaxe") || type.contains("shield")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Selects a death message using a priority system: killers > causes > biome > armed > fallback.
     * This ensures the message is thematically relevant rather than a random mix of categories.
     */
    private String selectMessage(String causeKey, String biomeKey, String killerKey, Entity killerEntity, Player player, String locationName, boolean armed) {
        var root = plugin.getConfig().getConfigurationSection("death_messages");
        if (root == null) {
            return null;
        }

        // Priority: killer-specific > cause-specific > biome-specific > armed-state > fallback
        List<String> templates;
        if (!"none".equals(killerKey)) {
            templates = getTemplates(root, "killers." + killerKey);
            if (!templates.isEmpty()) {
                return buildFinalMessage(player, locationName, killerKey, killerEntity, biomeKey, armed, templates, root);
            }
        }

        templates = getTemplates(root, "causes." + causeKey);
        if (!templates.isEmpty()) {
            return buildFinalMessage(player, locationName, killerKey, killerEntity, biomeKey, armed, templates, root);
        }

        templates = getTemplates(root, "biomes." + biomeKey);
        if (!templates.isEmpty()) {
            return buildFinalMessage(player, locationName, killerKey, killerEntity, biomeKey, armed, templates, root);
        }

        templates = getTemplates(root, "armed." + armed);
        if (!templates.isEmpty()) {
            return buildFinalMessage(player, locationName, killerKey, killerEntity, biomeKey, armed, templates, root);
        }

        templates = getTemplates(root, "fallback");
        if (!templates.isEmpty()) {
            return buildFinalMessage(player, locationName, killerKey, killerEntity, biomeKey, armed, templates, root);
        }

        return null;
    }

    /** Picks from the template list, avoids immediate repeats, fills placeholders and optionally appends a modifier flourish. */
    private String buildFinalMessage(Player player, String locationName, String killerKey, Entity killerEntity, String biomeKey, boolean armed, List<String> templates, org.bukkit.configuration.ConfigurationSection root) {
        UUID id = player.getUniqueId();
        String chosen = templates.get(random.nextInt(templates.size()));
        if (templates.size() > 1 && chosen.equals(lastTemplates.get(id))) {
            // Avoid repeating the exact same line back-to-back; a single re-roll is enough to break streaks.
            chosen = templates.get(random.nextInt(templates.size()));
        }
        lastTemplates.put(id, chosen);

        String killerName = resolveKillerName(killerKey, killerEntity, root);
        boolean killerNameIsProper = killerEntity instanceof Player;

        String core = fillPlaceholders(chosen, player.getName(), locationName, killerName, biomeKey, armed, killerNameIsProper);

        String modifier = pickRandom(root, "modifiers");
        if (modifier != null && !modifier.isBlank() && random.nextInt(100) < MODIFIER_CHANCE) {
            core = core + " " + fillPlaceholders(modifier, player.getName(), locationName, killerName, biomeKey, armed, killerNameIsProper);
        }

        return core;
    }

    private String resolveKillerName(String killerKey, Entity killerEntity, org.bukkit.configuration.ConfigurationSection root) {
        if ("none".equals(killerKey)) {
            return "";
        }
        if (killerEntity instanceof Player killerPlayer) {
            return killerPlayer.getName();
        }
        String killerName = pickRandom(root, "killer_synonyms." + killerKey);
        if (killerName == null || killerName.isBlank()) {
            killerName = killerKey.replace('_', ' ');
        }
        return killerName;
    }

    private List<String> getTemplates(org.bukkit.configuration.ConfigurationSection root, String path) {
        return root.getStringList(path);
    }

    private String pickRandom(org.bukkit.configuration.ConfigurationSection root, String path) {
        List<String> templates = root.getStringList(path);
        if (templates == null || templates.isEmpty()) {
            return null;
        }
        return templates.get(random.nextInt(templates.size()));
    }

    private String fillPlaceholders(String template, String playerName, String locationName, String killerName, String biomeKey, boolean armed, boolean killerNameIsProper) {
        return template.replace("{player}", playerName)
                .replace("{biome}", biomeKey.replace('_', ' '))
                .replace("{location}", locationName)
                .replace("{killer}", killerNameIsProper ? killerName : capitalize(killerName))
                .replace("{armed}", armed ? "armed" : "unarmed");
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
