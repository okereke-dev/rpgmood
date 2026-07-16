package com.okereke.rpgmood.mob;

import com.okereke.rpgmood.RPGMoodPlugin;
import com.okereke.rpgmood.farming.SeasonManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Applies advanced AI at spawn (creeper fuse, archer kite goal) and seasonal combat hooks
 * (winter armor / damage resist, summer burn on-hit).
 */
public final class AdvancedAiService implements Listener {

    private final RPGMoodPlugin plugin;
    private final NamespacedKey winterResistKey;
    private final NamespacedKey summerBurnKey;

    public AdvancedAiService(RPGMoodPlugin plugin) {
        this.plugin = plugin;
        this.winterResistKey = new NamespacedKey(plugin, "seasonal_winter");
        this.summerBurnKey = new NamespacedKey(plugin, "seasonal_summer_burn");
    }

    /** Call after scaling + affixes on a freshly spawned mob. */
    public void applyOnSpawn(LivingEntity entity, int level) {
        if (entity == null || level <= 0) return;

        applySeasonalSpawn(entity, level);
        applyCreeperFuse(entity, level);
        applyArcherKite(entity, level);
    }

    private void applySeasonalSpawn(LivingEntity entity, int level) {
        ConfigurationSection seasonal = plugin.getConfig().getConfigurationSection("mob_scaling.seasonal");
        if (seasonal == null || !seasonal.getBoolean("enabled", true)) return;
        SeasonManager seasons = plugin.getSeasonManager();
        if (seasons == null) return;

        SeasonManager.Season season = seasons.getCurrentSeason();
        if (season == SeasonManager.Season.WINTER) {
            double armorBonus = seasonal.getDouble("winter.armor-bonus", 2.0);
            AttributeInstance armor = entity.getAttribute(Attribute.ARMOR);
            if (armor != null && armorBonus > 0) {
                armor.setBaseValue(armor.getBaseValue() + armorBonus);
            }
            entity.getPersistentDataContainer().set(winterResistKey, PersistentDataType.BYTE, (byte) 1);
        } else if (season == SeasonManager.Season.SUMMER) {
            if (isFireOrDesertMob(entity)) {
                entity.getPersistentDataContainer().set(summerBurnKey, PersistentDataType.BYTE, (byte) 1);
            }
        }
        // Autumn Swift bonus is handled inside MobAffixService via getAutumnSwiftBonus()
    }

    private void applyCreeperFuse(LivingEntity entity, int level) {
        ConfigurationSection ai = plugin.getConfig().getConfigurationSection("mob_scaling.advanced_ai");
        if (ai == null || !ai.getBoolean("enabled", true)) return;
        ConfigurationSection creeperCfg = ai.getConfigurationSection("creeper");
        if (creeperCfg == null) return;
        if (!(entity instanceof Creeper creeper)) return;

        int minLevel = creeperCfg.getInt("min-level", 25);
        if (level < minLevel) return;

        int baseFuse = creeperCfg.getInt("base-fuse-ticks", 30);
        int minFuse = creeperCfg.getInt("min-fuse-ticks", 12);
        // Shorten fuse as level rises above min
        int shortened = baseFuse - (level - minLevel);
        creeper.setMaxFuseTicks(Math.max(minFuse, shortened));

        if (creeperCfg.getBoolean("advance-while-ignited", true)) {
            startCreeperAdvanceTask(creeper);
        }
    }

    private void startCreeperAdvanceTask(Creeper creeper) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!creeper.isValid() || creeper.isDead()) {
                    cancel();
                    return;
                }
                if (!creeper.isIgnited()) return;
                if (!(creeper.getTarget() instanceof Player player)) return;
                if (!player.isValid() || player.isDead()) return;
                creeper.getPathfinder().moveTo(player, 1.2);
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }

    private void applyArcherKite(LivingEntity entity, int level) {
        ConfigurationSection ai = plugin.getConfig().getConfigurationSection("mob_scaling.advanced_ai");
        if (ai == null || !ai.getBoolean("enabled", true)) return;
        ConfigurationSection kite = ai.getConfigurationSection("kite");
        if (kite == null) return;
        if (level < kite.getInt("min-level", 30)) return;
        if (!(entity instanceof Mob mob)) return;

        EntityType type = entity.getType();
        if (type != EntityType.SKELETON && type != EntityType.STRAY && type != EntityType.PILLAGER) return;

        double keep = kite.getDouble("keep-distance", 8.0);
        Bukkit.getMobGoals().addGoal(mob, 3, new ArcherKiteGoal(plugin, mob, keep));
    }

    private static boolean isFireOrDesertMob(LivingEntity entity) {
        EntityType type = entity.getType();
        if (type == EntityType.BLAZE || type == EntityType.MAGMA_CUBE || type == EntityType.HOGLIN
                || type == EntityType.PIGLIN || type == EntityType.PIGLIN_BRUTE
                || type == EntityType.WITHER_SKELETON || type == EntityType.GHAST) {
            return true;
        }
        String biome = entity.getLocation().getBlock().getBiome().name();
        return biome.contains("DESERT") || biome.contains("BADLANDS") || biome.contains("NETHER");
    }

    /** Winter passive: slight damage reduction for tagged mobs. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWinterResist(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;
        Byte tag = living.getPersistentDataContainer().get(winterResistKey, PersistentDataType.BYTE);
        if (tag == null || tag != 1) return;
        event.setDamage(event.getDamage() * 0.85);
    }

    /** Summer burn on-hit for tagged fire/desert mobs. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSummerBurn(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        LivingEntity mob = resolveMob(event.getDamager());
        if (mob == null) return;
        Byte tag = mob.getPersistentDataContainer().get(summerBurnKey, PersistentDataType.BYTE);
        if (tag == null || tag != 1) return;

        ConfigurationSection seasonal = plugin.getConfig().getConfigurationSection("mob_scaling.seasonal");
        if (seasonal == null) return;
        double chance = seasonal.getDouble("summer.burn-chance", 0.35);
        if (Math.random() > chance) return;
        int ticks = seasonal.getInt("summer.burn-ticks", 60);
        player.setFireTicks(Math.max(player.getFireTicks(), ticks));
    }

    private static LivingEntity resolveMob(org.bukkit.entity.Entity damager) {
        if (damager instanceof LivingEntity living) return living;
        if (damager instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof LivingEntity shooter) {
            return shooter;
        }
        return null;
    }

    /** Autumn: extra chance to force Swift into the affix roll. */
    public double getAutumnSwiftBonus() {
        ConfigurationSection seasonal = plugin.getConfig().getConfigurationSection("mob_scaling.seasonal");
        if (seasonal == null || !seasonal.getBoolean("enabled", true)) return 0.0;
        SeasonManager seasons = plugin.getSeasonManager();
        if (seasons == null || seasons.getCurrentSeason() != SeasonManager.Season.AUTUMN) return 0.0;
        return seasonal.getDouble("autumn.swift-chance-bonus", 0.20);
    }
}
