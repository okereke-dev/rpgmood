package com.okereke.rpgmood;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Applies subtle, discreet coloured particle auras around scaled mobs based on their level.
 * Only active when a player is within 30 blocks, particles are small and at feet level.
 *
 * Level tiers:
 *   1-7   — no particles (not dangerous enough)
 *   8-14  — barely visible blue shimmer (SPELL_MOB)
 *   15-24 — tiny warm yellow glow (REDSTONE 0.4f)
 *   25-34 — small orange ember (REDSTONE 0.6f)
 *   35+   — modest red glow (REDSTONE 0.8f)
 */
public class MobAuraEffect extends BukkitRunnable {

    private static final double AURA_RADIUS = 0.3;
    private static final int PARTICLE_COUNT = 1;
    private static final int PROXIMITY_RADIUS_SQ = 900; // 30 blocks squared

    /** Tripped on the first spawnParticle failure so we stop retrying (and throwing) every tick for every mob — some server/Paper builds reject the DustOptions data type this uses. */
    private static volatile boolean particlesDisabled = false;

    private final RPGMoodPlugin plugin;

    public MobAuraEffect(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("mob_scaling.particle_aura", true)) {
            return;
        }

        plugin.getServer().getWorlds().forEach(world ->
            world.getLivingEntities().stream()
                .filter(entity -> entity.getPersistentDataContainer()
                    .has(plugin.getMobScalingService().getLevelKey(), org.bukkit.persistence.PersistentDataType.INTEGER))
                .filter(entity -> isNearAnyPlayer(entity))
                .forEach(this::spawnAura)
        );
    }

    /** Only show auras for mobs within range of at least one player. */
    private boolean isNearAnyPlayer(LivingEntity entity) {
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(entity.getLocation()) <= PROXIMITY_RADIUS_SQ) {
                return true;
            }
        }
        return false;
    }

    private void spawnAura(LivingEntity entity) {
        if (particlesDisabled) return;

        Integer level = entity.getPersistentDataContainer()
                .get(plugin.getMobScalingService().getLevelKey(), org.bukkit.persistence.PersistentDataType.INTEGER);
        if (level == null || level <= 0) return;

        if (entity.isDead() || !entity.isValid()) return;

        Particle particle;
        Object data = null;

        if (level >= 35) {
            // 🔴 Elite — modest red glow at feet
            particle = Particle.REDSTONE;
            data = new Particle.DustOptions(Color.fromRGB(200, 30, 30), 0.8f);
        } else if (level >= 25) {
            // 🟠 Dangerous — small orange ember
            particle = Particle.REDSTONE;
            data = new Particle.DustOptions(Color.fromRGB(220, 120, 20), 0.6f);
        } else if (level >= 15) {
            // 🟡 Moderate — tiny warm yellow glow
            particle = Particle.REDSTONE;
            data = new Particle.DustOptions(Color.fromRGB(200, 200, 50), 0.4f);
        } else if (level >= 8) {
            // 🔵 Enhanced — barely visible blue shimmer near feet
            particle = Particle.SPELL_MOB;
            data = new Particle.DustOptions(Color.fromRGB(60, 120, 255), 0.3f);
        } else {
            // ⚪ Low levels (1-7) — no particles, barely scaled
            return;
        }

        try {
            if (data != null) {
                entity.getWorld().spawnParticle(particle, entity.getLocation().add(0, 0.5, 0),
                        PARTICLE_COUNT, AURA_RADIUS, AURA_RADIUS, AURA_RADIUS, 0, data);
            } else {
                entity.getWorld().spawnParticle(particle, entity.getLocation().add(0, 0.5, 0),
                        PARTICLE_COUNT, AURA_RADIUS, AURA_RADIUS, AURA_RADIUS, 0);
            }
        } catch (IllegalArgumentException e) {
            particlesDisabled = true;
            plugin.getLogger().warning("Mob level particle auras disabled — this server's Particle API rejected the expected data type (" + e.getMessage() + "). This is a server/Paper-version incompatibility, not a config issue; scaled mobs will still work, just without the color aura.");
        }
    }
}
