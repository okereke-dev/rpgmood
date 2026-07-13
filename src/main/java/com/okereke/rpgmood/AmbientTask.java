package com.okereke.rpgmood;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AmbientTask extends BukkitRunnable {

    private static final long MAP_CLEANUP_INTERVAL = 6000L; // 5 minutes in ticks
    private static final long EVENT_EXPIRY_MILLIS = 3600000L; // 1 hour without updates

    private final RPGMoodPlugin plugin;
    private final Map<String, Long> lastTriggeredEvents = new HashMap<>();
    private final Map<String, String> lastWeatherType = new HashMap<>();
    private final Random random = new Random();
    private int tickCounter = 0;

    public AmbientTask(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!isEnabled(player)) {
                continue;
            }
            checkTimeEvents(player);
            checkWeatherEvents(player);
            checkAmbientSounds(player);
            checkStormEffects(player);
            checkNetherEvents(player);
        }

        tickCounter++;
        if (tickCounter >= MAP_CLEANUP_INTERVAL) {
            tickCounter = 0;
            evictStaleEntries();
        }
    }

    /** Removes entries from tracking maps that haven't been updated in over an hour. */
    private void evictStaleEntries() {
        long now = System.currentTimeMillis();
        long cutoff = now - EVENT_EXPIRY_MILLIS;

        lastTriggeredEvents.values().removeIf(timestamp -> timestamp < cutoff);

        // Preserve weather type entries for worlds that still exist; remove stale world entries
        Iterator<Map.Entry<String, String>> wit = lastWeatherType.entrySet().iterator();
        while (wit.hasNext()) {
            if (Bukkit.getWorld(wit.next().getKey()) == null) {
                wit.remove();
            }
        }
    }

    private boolean isEnabled(Player player) {
        return plugin.getConfigManager().getConfigValues().getBoolean("player_effects." + player.getUniqueId(), true);
    }

    private void checkTimeEvents(Player player) {
        var timeSection = plugin.getConfigManager().getTriggers().getConfigurationSection("time_events");
        if (timeSection == null) {
            return;
        }

        World world = player.getWorld();
        long worldTime = world.getTime() % 24000L;
        long now = System.currentTimeMillis();

        for (String eventKey : timeSection.getKeys(false)) {
            var eventSection = timeSection.getConfigurationSection(eventKey);
            if (eventSection == null) {
                continue;
            }

            int triggerTime = eventSection.getInt("time", 0);
            long diff = Math.abs(worldTime - triggerTime);
            long circularDiff = Math.min(diff, 24000L - diff);
            if (circularDiff <= 100) {
                Long last = lastTriggeredEvents.get(eventKey);
                if (last != null && now - last < 600000L) {
                    continue;
                }

                lastTriggeredEvents.put(eventKey, now);
                String messageStr = eventSection.getString("message", "");
                String sound = eventSection.getString("sound", "entity.player.levelup");
                plugin.getMessageService().send(player, messageStr);
                player.playSound(player.getLocation(), sound, 0.8f, 1.0f);
            }
        }
    }

    private void checkWeatherEvents(Player player) {
        var weatherSection = plugin.getConfigManager().getTriggers().getConfigurationSection("weather_events");
        if (weatherSection == null) {
            return;
        }

        World world = player.getWorld();
        String currentWeather = world.isThundering() ? "thunder" : world.hasStorm() ? "rain" : "clear";
        String worldKey = world.getName();
        String lastWeather = lastWeatherType.get(worldKey);
        if (lastWeather == null) {
            lastWeatherType.put(worldKey, currentWeather);
            return;
        }

        if (currentWeather.equals(lastWeather)) {
            return;
        }

        long now = System.currentTimeMillis();
        for (String eventKey : weatherSection.getKeys(false)) {
            var eventSection = weatherSection.getConfigurationSection(eventKey);
            if (eventSection == null) {
                continue;
            }

            String weather = eventSection.getString("weather", "").toLowerCase();
            String state = eventSection.getString("state", "").toLowerCase();
            if (weather.isBlank() || state.isBlank()) {
                continue;
            }

            boolean triggered = false;
            if ("start".equals(state) && !lastWeather.equals(weather) && currentWeather.equals(weather)) {
                triggered = true;
            }
            if ("stop".equals(state) && lastWeather.equals(weather) && !currentWeather.equals(weather)) {
                triggered = true;
            }

            if (!triggered) {
                continue;
            }

            String eventId = "weather_" + eventKey;
            Long last = lastTriggeredEvents.get(eventId);
            if (last != null && now - last < 600000L) {
                continue;
            }

            lastTriggeredEvents.put(eventId, now);
            String messageStr = eventSection.getString("message", "");
            String sound = eventSection.getString("sound", "entity.player.levelup");
            plugin.getMessageService().send(player, messageStr);
            player.playSound(player.getLocation(), sound, 0.8f, 1.0f);
        }

        lastWeatherType.put(worldKey, currentWeather);
    }

    /** Low-probability day/night flavor sound near the player — birds by day, eerie ambience by night. Purely cosmetic, no message. */
    private void checkAmbientSounds(Player player) {
        if (!plugin.getConfig().getBoolean("ambient_sounds.enabled", true)) {
            return;
        }
        double chance = plugin.getConfig().getDouble("ambient_sounds.chance", 0.02);
        if (random.nextDouble() > chance) {
            return;
        }

        long worldTime = player.getWorld().getTime() % 24000L;
        boolean isNight = worldTime >= 13000L && worldTime < 23000L;
        List<String> sounds = plugin.getConfig().getStringList(isNight ? "ambient_sounds.night" : "ambient_sounds.day");
        if (sounds.isEmpty()) {
            return;
        }

        String sound = sounds.get(random.nextInt(sounds.size()));
        player.playSound(player.getLocation(), sound, 0.5f, 1.0f);
    }

    /**
     * Mechanical weather effects while a storm is live — checked against the world's actual
     * live weather state each tick rather than one-shot start/stop triggers, so there's no
     * separate state to track or accidentally leave active if a storm ends unusually.
     */
    private void checkStormEffects(Player player) {
        if (!plugin.getConfig().getBoolean("weather_effects.enabled", true)) {
            return;
        }
        World world = player.getWorld();

        if (world.isThundering()) {
            double fogChance = plugin.getConfig().getDouble("weather_effects.fog_chance", 0.05);
            if (random.nextDouble() < fogChance) {
                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.DARKNESS, 60, 0, true, false));
            }
        }

        if (world.hasStorm()) {
            double windChance = plugin.getConfig().getDouble("weather_effects.wind_chance", 0.03);
            if (random.nextDouble() < windChance && player.getLocation().getBlock().getLightFromSky() > 0) {
                double strength = plugin.getConfig().getDouble("weather_effects.wind_strength", 0.15);
                double angle = random.nextDouble() * Math.PI * 2;
                player.setVelocity(player.getVelocity().add(new org.bukkit.util.Vector(
                        Math.cos(angle) * strength, 0, Math.sin(angle) * strength)));
            }
        }
    }

    /** Nether-only ambient hazard — there's no vanilla weather there, so this is a standalone chance-based trigger instead of a weather_events entry. */
    private void checkNetherEvents(Player player) {
        if (player.getWorld().getEnvironment() != World.Environment.NETHER) {
            return;
        }
        var section = plugin.getConfigManager().getTriggers().getConfigurationSection("nether_events.acid_rain");
        if (section == null || !section.getBoolean("enabled", true)) {
            return;
        }

        double chance = section.getDouble("chance", 0.01);
        if (random.nextDouble() > chance) {
            return;
        }

        double damage = section.getDouble("damage", 1.0);
        player.damage(damage);
        String messageStr = section.getString("message", "");
        plugin.getMessageService().send(player, messageStr);
    }
}
