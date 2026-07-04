package com.ricardo.rpgmood;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.StructureType;

import java.util.Locale;

public class MobScalingService {

    private final RPGMoodPlugin plugin;

    public MobScalingService(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("mob_scaling.enabled", true);
    }

    public boolean shouldScale(LivingEntity entity) {
        if (!isEnabled()) {
            return false;
        }
        if (!plugin.getConfig().getBoolean("mob_scaling.hostile-only", true)) {
            return true;
        }
        return entity instanceof Monster;
    }

    public void applyScaling(LivingEntity entity) {
        if (entity == null || !shouldScale(entity) || entity.getScoreboardTags().contains("rpgmood_scaled")) {
            return;
        }

        int level = calculateLevel(entity);
        if (level <= 0) {
            return;
        }

        double health = Math.max(10.0, 14.0 + (level - 1) * plugin.getConfig().getDouble("mob_scaling.health_per_level", 2.0));
        double damage = Math.max(0.4, 0.6 + (level - 1) * plugin.getConfig().getDouble("mob_scaling.damage_per_level", 0.12));
        double armor = Math.max(0.0, (level - 1) * plugin.getConfig().getDouble("mob_scaling.armor_per_level", 0.25));
        double speedBonus = Math.max(0.0, (level - 1) * plugin.getConfig().getDouble("mob_scaling.speed_per_level", 0.0015));

        AttributeInstance maxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(health);
            entity.setHealth(Math.min(entity.getHealth(), health));
        }

        AttributeInstance damageAttribute = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (damageAttribute != null) {
            damageAttribute.setBaseValue(damage);
        }

        AttributeInstance armorAttribute = entity.getAttribute(Attribute.GENERIC_ARMOR);
        if (armorAttribute != null) {
            armorAttribute.setBaseValue(Math.max(0.0, armor));
        }

        AttributeInstance speedAttribute = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttribute != null) {
            speedAttribute.setBaseValue(Math.max(0.01, speedAttribute.getBaseValue() + speedBonus));
        }

        entity.addScoreboardTag("rpgmood_scaled");
        entity.setCustomName(ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("mob_scaling.name_format", "&cLv. {level} {name}")
                .replace("{level}", String.valueOf(level))
                .replace("{name}", entity.getName())));
        entity.setCustomNameVisible(true);
    }

    public int calculateLevel(LivingEntity entity) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("mob_scaling");
        if (section == null) {
            return 1;
        }

        int baseLevel = getBaseLevel(entity.getType());
        double distanceFromSpawn = entity.getLocation().distance(entity.getWorld().getSpawnLocation());
        int biomeBonus = getBiomeBonus(entity.getLocation());
        int structureBonus = getStructureBonus(entity.getLocation());
        int nearbyPlayers = countNearbyPlayers(entity);

        return calculateDifficultyLevel(
                baseLevel,
                distanceFromSpawn,
                biomeBonus,
                structureBonus,
                nearbyPlayers,
                section.getInt("max-level", 40),
                section.getDouble("radius.step_distance", 180.0),
                section.getInt("players_bonus_per_player", 1)
        );
    }

    public static int calculateDifficultyLevel(int baseLevel, double distanceFromSpawn, int biomeBonus, int structureBonus, int nearbyPlayers, int maxLevel, double stepDistance, int playerBonusPerPlayer) {
        int radialLevel = (int) Math.floor(distanceFromSpawn / stepDistance);
        int adjustedBase = Math.max(0, baseLevel - 2);
        int total = adjustedBase + Math.max(0, radialLevel - 1) + biomeBonus + structureBonus + nearbyPlayers * playerBonusPerPlayer;
        return Math.max(1, Math.min(maxLevel, total));
    }

    private int getBaseLevel(EntityType type) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("mob_scaling.base-levels");
        if (section == null) {
            return 1;
        }

        String key = type.name();
        return section.getInt(key, 1);
    }

    private int getBiomeBonus(Location location) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("mob_scaling.biome-bonuses");
        if (section == null) {
            return 0;
        }

        return section.getInt(location.getBlock().getBiome().name(), 0);
    }

    private int getStructureBonus(Location location) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("mob_scaling.structure-bonuses");
        if (section == null) {
            return 0;
        }

        for (String key : section.getKeys(false)) {
            if (key == null || key.isBlank()) {
                continue;
            }
            String normalizedKey = key.toLowerCase(Locale.ROOT).trim();
            if (normalizedKey.isBlank()) {
                continue;
            }
            NamespacedKey namespacedKey;
            try {
                namespacedKey = NamespacedKey.fromString("minecraft:" + normalizedKey);
            } catch (IllegalArgumentException ex) {
                continue;
            }
            StructureType structureType = Bukkit.getRegistry(StructureType.class).get(namespacedKey);
            if (structureType != null && location.getWorld().locateNearestStructure(location, structureType, 64, false) != null) {
                return section.getInt(key, 0);
            }
        }
        return 0;
    }

    private int countNearbyPlayers(LivingEntity entity) {
        int count = 0;
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(entity.getLocation()) <= 900.0) {
                count++;
            }
        }
        return count;
    }
}
