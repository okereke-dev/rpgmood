package com.ricardo.rpgmood;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class AmbientTask extends BukkitRunnable {

    private final RPGMoodPlugin plugin;
    private final Map<String, Long> lastTriggeredEvents = new HashMap<>();
    private final Map<String, String> lastWeatherType = new HashMap<>();

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
            if (Math.abs(worldTime - triggerTime) <= 100) {
                Long last = lastTriggeredEvents.get(eventKey);
                if (last != null && now - last < 600000L) {
                    continue;
                }

                lastTriggeredEvents.put(eventKey, now);
                Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(eventSection.getString("message", ""));
                String sound = eventSection.getString("sound", "entity.player.levelup");
                player.sendMessage(message);
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
            Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(eventSection.getString("message", ""));
            String sound = eventSection.getString("sound", "entity.player.levelup");
            player.sendMessage(message);
            player.playSound(player.getLocation(), sound, 0.8f, 1.0f);
        }

        lastWeatherType.put(worldKey, currentWeather);
    }

    private String capitalize(String value) {
        String[] parts = value.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return builder.toString();
    }
}
