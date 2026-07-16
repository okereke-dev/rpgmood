package com.okereke.rpgmood.mob;

import com.okereke.rpgmood.RPGMoodPlugin;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Recalculates bow/crossbow projectile vectors by mob level:
 * clumsy (spread), laser (eyes), predictive (lead the player's velocity).
 */
public final class BowAimListener implements Listener {

    private final RPGMoodPlugin plugin;
    private final Random random = new Random();
    private final Map<UUID, BukkitTask> tracerTasks = new ConcurrentHashMap<>();

    public BowAimListener(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("mob_scaling.archery");
        if (section == null || !section.getBoolean("enabled", true)) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) return;

        Integer level = mob.getPersistentDataContainer().get(
                plugin.getMobScalingService().getLevelKey(), PersistentDataType.INTEGER);
        if (level == null || level <= 0) return;

        int clumsyMax = section.getInt("clumsy-max-level", 7);
        int laserMin = section.getInt("laser-min-level", 15);
        int predictiveMin = section.getInt("predictive-min-level", 30);
        double arrowSpeed = section.getDouble("arrow-speed", 1.6);
        double clumsySpread = section.getDouble("clumsy-spread", 0.35);

        LivingEntity target = mob.getTarget();
        if (!(target instanceof Player player) || !player.isValid()) {
            if (level <= clumsyMax) {
                applySpread(arrow, clumsySpread);
            }
            return;
        }

        if (level <= clumsyMax) {
            applySpread(arrow, clumsySpread);
            return;
        }

        if (level < laserMin) {
            return; // vanilla mid band
        }

        Vector aimPoint;
        if (level >= predictiveMin) {
            aimPoint = predictIntercept(player, mob.getEyeLocation().toVector(), arrowSpeed);
        } else {
            aimPoint = player.getEyeLocation().toVector();
        }

        Vector direction = aimPoint.subtract(mob.getEyeLocation().toVector());
        if (direction.lengthSquared() < 1.0E-6) return;
        direction.normalize().multiply(Math.max(0.5, arrow.getVelocity().length()));
        arrow.setVelocity(direction);

        if (level >= predictiveMin && section.getBoolean("tracer-particles", true)) {
            startTracer(arrow);
        }
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        stopTracer(projectile.getUniqueId());

        if (!(event.getHitEntity() instanceof Player player)) return;
        if (!(projectile instanceof AbstractArrow)) return;
        if (!(projectile.getShooter() instanceof LivingEntity shooter)) return;

        ConfigurationSection seasonal = plugin.getConfig().getConfigurationSection("mob_scaling.seasonal");
        if (seasonal == null || !seasonal.getBoolean("enabled", true)) return;
        if (plugin.getSeasonManager() == null) return;
        if (plugin.getSeasonManager().getCurrentSeason() != com.okereke.rpgmood.farming.SeasonManager.Season.WINTER) {
            return;
        }

        Integer level = shooter.getPersistentDataContainer().get(
                plugin.getMobScalingService().getLevelKey(), PersistentDataType.INTEGER);
        if (level == null || level <= 0) return;

        int ticks = seasonal.getInt("winter.arrow-slowness-ticks", 40);
        int amp = seasonal.getInt("winter.arrow-slowness-amplifier", 0);
        if (ticks > 0) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SLOWNESS, ticks, amp, false, true, true));
        }
    }

    private static Vector predictIntercept(Player player, Vector from, double arrowSpeed) {
        Vector targetPos = player.getEyeLocation().toVector();
        Vector velocity = player.getVelocity().clone();
        double distance = targetPos.distance(from);
        double flightTicks = arrowSpeed > 0.01 ? distance / arrowSpeed : 0;
        // velocity is blocks/tick — lead by estimated flight time
        return targetPos.add(velocity.multiply(flightTicks));
    }

    private void applySpread(AbstractArrow arrow, double spread) {
        Vector v = arrow.getVelocity();
        v.add(new Vector(
                (random.nextDouble() - 0.5) * 2 * spread,
                (random.nextDouble() - 0.5) * spread * 0.5,
                (random.nextDouble() - 0.5) * 2 * spread
        ));
        arrow.setVelocity(v);
    }

    private void startTracer(AbstractArrow arrow) {
        UUID id = arrow.getUniqueId();
        stopTracer(id);
        BukkitTask task = new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!arrow.isValid() || arrow.isDead() || arrow.isOnGround() || ticks++ > 100) {
                    stopTracer(id);
                    cancel();
                    return;
                }
                arrow.getWorld().spawnParticle(Particle.SCULK_SOUL, arrow.getLocation(), 1, 0.02, 0.02, 0.02, 0);
            }
        }.runTaskTimer(plugin, 0L, 1L);
        tracerTasks.put(id, task);
    }

    private void stopTracer(UUID id) {
        BukkitTask task = tracerTasks.remove(id);
        if (task != null) task.cancel();
    }
}
