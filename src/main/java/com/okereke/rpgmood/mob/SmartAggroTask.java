package com.okereke.rpgmood.mob;

import com.okereke.rpgmood.RPGMoodPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sync every-second scan that forces high-level mobs to aggro players within budgeted
 * limits. Iterates players → nearby mobs (never the whole world). All entity access stays
 * on the main thread — Paper forbids setTarget off-thread.
 */
public final class SmartAggroTask extends BukkitRunnable {

    private final RPGMoodPlugin plugin;
    private final Map<UUID, Long> cooldownUntilTick = new ConcurrentHashMap<>();

    public SmartAggroTask(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("mob_scaling.smart_aggro");
        if (section == null || !section.getBoolean("enabled", true)) return;
        if (!plugin.getMobScalingService().isEnabled()) return;

        int losMin = section.getInt("los-min-level", 15);
        double losRadius = section.getDouble("los-radius", 28.0);
        int omniMin = section.getInt("omniscient-min-level", 30);
        double omniRadius = section.getDouble("omniscient-radius", 36.0);
        int maxRetargets = Math.max(1, section.getInt("max-retargets-per-tick", 40));
        long cooldownTicks = Math.max(1L, section.getLong("cooldown-ticks", 40L));

        long now = plugin.getServer().getCurrentTick();
        int retargets = 0;
        double scanRadius = Math.max(losRadius, omniRadius);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isValid() || player.isDead()) continue;
            Location playerLoc = player.getLocation();

            for (LivingEntity nearby : player.getWorld().getNearbyLivingEntities(playerLoc, scanRadius)) {
                if (retargets >= maxRetargets) return;
                if (!(nearby instanceof Mob mob) || !(nearby instanceof Monster)) continue;
                if (!mob.isValid() || mob.isDead()) continue;

                Integer level = mob.getPersistentDataContainer().get(
                        plugin.getMobScalingService().getLevelKey(), PersistentDataType.INTEGER);
                if (level == null || level < losMin) continue;

                Long until = cooldownUntilTick.get(mob.getUniqueId());
                if (until != null && until > now) continue;

                LivingEntity current = mob.getTarget();
                if (current != null && current.isValid() && !current.isDead()) continue;

                double distSq = mob.getLocation().distanceSquared(playerLoc);
                boolean shouldAggro = false;
                if (level >= omniMin && distSq <= omniRadius * omniRadius) {
                    shouldAggro = true;
                } else if (level >= losMin && distSq <= losRadius * losRadius && mob.hasLineOfSight(player)) {
                    shouldAggro = true;
                }

                if (!shouldAggro) continue;

                mob.setTarget(player);
                cooldownUntilTick.put(mob.getUniqueId(), now + cooldownTicks);
                retargets++;
            }
        }

        // Prune stale cooldowns occasionally
        if (now % 200 == 0) {
            cooldownUntilTick.entrySet().removeIf(e -> e.getValue() <= now);
        }
    }
}
