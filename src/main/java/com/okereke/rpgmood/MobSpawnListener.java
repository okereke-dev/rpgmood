package com.okereke.rpgmood;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

import java.util.Locale;

public class MobSpawnListener implements Listener {

    private final RPGMoodPlugin plugin;
    private long lastAnnouncementMillis = 0L;

    public MobSpawnListener(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (event == null) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (entity == null || event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            return;
        }
        if (isProtectedFromSpawn(entity)) {
            event.setCancelled(true);
            return;
        }
        if (plugin.getMobScalingService() == null) {
            return;
        }
        int level = plugin.getMobScalingService().applyScaling(entity);
        if (level > 0) {
            plugin.getMobAffixService().rollAndApply(entity, level);
            maybeAnnounce(entity, level);
        }
    }

    private boolean isProtectedFromSpawn(LivingEntity entity) {
        if (!(entity instanceof Monster)) {
            return false;
        }
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("spawn_protection");
        if (section == null || !section.getBoolean("enabled", true)) {
            return false;
        }
        double radius = section.getDouble("radius", 64);
        double distance = entity.getLocation().distance(entity.getWorld().getSpawnLocation());
        return distance < radius;
    }

    private void maybeAnnounce(LivingEntity entity, int level) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("mob_scaling.announcement");
        if (section == null || !section.getBoolean("enabled", false)) {
            return;
        }

        if (level < section.getInt("min-level", 25)) {
            return;
        }

        long cooldownMillis = section.getLong("cooldown-seconds", 120L) * 1000L;
        long now = System.currentTimeMillis();
        if (now - lastAnnouncementMillis < cooldownMillis) {
            return;
        }
        lastAnnouncementMillis = now;

        String mobName = prettify(entity.getType().name());
        String biomeName = prettify(entity.getLocation().getBlock().getBiome().name());
        String message = section.getString("message", "&c⚠ A Level {level} {name} has emerged in {biome}!")
                .replace("{level}", String.valueOf(level))
                .replace("{name}", mobName)
                .replace("{biome}", biomeName);

        Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&', message));
    }

    private String prettify(String enumName) {
        String[] parts = enumName.toLowerCase(Locale.ROOT).split("_");
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
