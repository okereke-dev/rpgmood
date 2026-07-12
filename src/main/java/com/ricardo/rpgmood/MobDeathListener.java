package com.ricardo.rpgmood;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class MobDeathListener implements Listener {

    private final RPGMoodPlugin plugin;

    /** Transient: mob UUIDs that have ever damaged a player, for the Untouchable achievement. Evicted on death regardless of outcome. */
    private final Set<UUID> mobsThatDamagedAPlayer = new HashSet<>();

    public MobDeathListener(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityDamagePlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        Entity damager = event.getDamager();
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity shooter) {
            damager = shooter;
        }
        if (damager instanceof LivingEntity) {
            mobsThatDamagedAPlayer.add(damager.getUniqueId());
        }
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer != null) {
            plugin.getAchievementManager().onBossKilled(killer, entity.getType());
        }

        // Evict regardless of outcome so this never grows unbounded.
        boolean tookNoDamageFromMob = !mobsThatDamagedAPlayer.remove(entity.getUniqueId());

        if (killer == null) {
            return;
        }

        Integer level = entity.getPersistentDataContainer().get(plugin.getMobScalingService().getLevelKey(), PersistentDataType.INTEGER);
        if (level == null || level <= 0) {
            return;
        }

        int bonusXpPerLevel = plugin.getConfig().getInt("mob_scaling.bonus_xp_per_level", 0);
        if (bonusXpPerLevel > 0) {
            event.setDroppedExp(event.getDroppedExp() + level * bonusXpPerLevel);
        }

        plugin.getPlayerStatsService().recordMobKill(killer, level);
        plugin.getAchievementManager().onScaledMobKill(killer, level);

        if (tookNoDamageFromMob) {
            plugin.getAchievementManager().onUndamagedKill(killer, level);
        }
    }

    /** Runs after every other plugin's EntityDeathEvent listener (e.g. RPGLoot's drop injection) so event.getDrops() is final. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMobDeathMonitor(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null) {
            return;
        }

        Integer level = entity.getPersistentDataContainer().get(plugin.getMobScalingService().getLevelKey(), PersistentDataType.INTEGER);
        if (level == null || level <= 0) {
            return;
        }

        plugin.getAchievementManager().onScaledMobKillWithDrops(killer, level, event.getDrops());
    }
}
