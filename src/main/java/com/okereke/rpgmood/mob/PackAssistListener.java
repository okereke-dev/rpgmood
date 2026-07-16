package com.okereke.rpgmood.mob;

import com.okereke.rpgmood.RPGMoodPlugin;
import org.bukkit.Chunk;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Level 20+ pack assist: when a scaled mob is hit by a player, up to N same-type allies
 * in radius set that player as target. Chunk-keyed cooldown prevents pathfinding storms.
 */
public final class PackAssistListener implements Listener {

    private final RPGMoodPlugin plugin;
    private final Map<Long, Long> chunkCooldownUntilMs = new ConcurrentHashMap<>();

    public PackAssistListener(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobDamaged(EntityDamageByEntityEvent event) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("mob_scaling.advanced_ai");
        if (section == null || !section.getBoolean("enabled", true)) return;
        ConfigurationSection pack = section.getConfigurationSection("pack");
        if (pack == null) return;

        if (!(event.getEntity() instanceof Mob victim) || !(victim instanceof Monster)) return;

        Player attacker = resolvePlayer(event.getDamager());
        if (attacker == null) return;

        Integer level = victim.getPersistentDataContainer().get(
                plugin.getMobScalingService().getLevelKey(), PersistentDataType.INTEGER);
        int minLevel = pack.getInt("min-level", 20);
        if (level == null || level < minLevel) return;

        Chunk chunk = victim.getLocation().getChunk();
        long key = chunkKey(chunk);
        long now = System.currentTimeMillis();
        Long until = chunkCooldownUntilMs.get(key);
        if (until != null && until > now) return;

        double radius = pack.getDouble("radius", 20.0);
        int maxAllies = Math.max(1, pack.getInt("max-allies", 5));
        long cooldownMs = Math.max(500L, pack.getLong("chunk-cooldown-seconds", 3L) * 1000L);

        int called = 0;
        for (LivingEntity nearby : victim.getWorld().getNearbyLivingEntities(victim.getLocation(), radius)) {
            if (called >= maxAllies) break;
            if (!(nearby instanceof Mob ally) || nearby == victim) continue;
            if (nearby.getType() != victim.getType()) continue;
            Integer allyLevel = nearby.getPersistentDataContainer().get(
                    plugin.getMobScalingService().getLevelKey(), PersistentDataType.INTEGER);
            if (allyLevel == null || allyLevel < minLevel) continue;

            LivingEntity current = ally.getTarget();
            if (current != null && current.isValid() && !current.isDead()) continue;

            ally.setTarget(attacker);
            called++;
        }

        if (called > 0) {
            chunkCooldownUntilMs.put(key, now + cooldownMs);
        }
    }

    private static Player resolvePlayer(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private static long chunkKey(Chunk chunk) {
        return (((long) chunk.getX()) << 32) ^ (chunk.getZ() & 0xffffffffL)
                ^ (((long) chunk.getWorld().getUID().getMostSignificantBits()) << 16);
    }
}
