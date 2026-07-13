package com.okereke.rpgmood.mob;

import com.okereke.rpgmood.RPGMoodPlugin;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles the "reversed" on-hit affixes — a Bleeding/Poisonous/Chilling mob inflicts its
 * status on the player it hits, the opposite direction of RPGLoot's own player-applies-bleed-
 * to-mob combat mechanic. The bleed DOT here is a from-scratch {@link BukkitRunnable}, not a
 * call into RPGLoot — same shape as its pattern, no dependency on it.
 */
public class MobAffixCombatListener implements Listener {

    private final RPGMoodPlugin plugin;
    private final Random random = new Random();

    /** Guards a player's own bleed-tick damage from re-triggering this listener — the tick's player.damage() call fires a fresh EntityDamageByEntityEvent. */
    private final Set<UUID> bleedingPlayers = ConcurrentHashMap.newKeySet();
    private final Map<UUID, BukkitTask> activeBleeds = new ConcurrentHashMap<>();

    public MobAffixCombatListener(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMobHitsPlayer(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (bleedingPlayers.contains(player.getUniqueId())) return;

        LivingEntity mob = resolveMob(event.getDamager());
        if (mob == null) return;

        ConfigurationSection roster = plugin.getConfig().getConfigurationSection("mob_scaling.affixes.roster");

        if (plugin.getMobAffixService().hasAffix(mob, MobAffix.BLEEDING)) {
            tryBleed(mob, player, roster);
        }
        if (plugin.getMobAffixService().hasAffix(mob, MobAffix.POISONOUS)) {
            tryPoison(mob, player, roster);
        }
        if (plugin.getMobAffixService().hasAffix(mob, MobAffix.CHILLING)) {
            tryChill(player, roster);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        BukkitTask task = activeBleeds.remove(uuid);
        if (task != null) task.cancel();
        bleedingPlayers.remove(uuid);
    }

    private LivingEntity resolveMob(Entity damager) {
        if (damager instanceof LivingEntity living) return living;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity shooter) {
            return shooter;
        }
        return null;
    }

    private void tryBleed(LivingEntity mob, Player player, ConfigurationSection roster) {
        double procChance = roster != null ? roster.getDouble("BLEEDING.proc-chance", 0.25) : 0.25;
        if (random.nextDouble() >= procChance) return;

        double tickDamagePct = roster != null ? roster.getDouble("BLEEDING.tick-damage-pct", 0.30) : 0.30;
        int durationTicks = roster != null ? roster.getInt("BLEEDING.duration-ticks", 100) : 100;

        AttributeInstance damageAttr = mob.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        double baseDamage = damageAttr != null ? damageAttr.getValue() : 2.0;
        double tickDamage = Math.max(0.5, baseDamage * tickDamagePct);

        UUID uuid = player.getUniqueId();
        BukkitTask previous = activeBleeds.remove(uuid);
        if (previous != null) previous.cancel();

        int totalTicks = Math.max(1, durationTicks / 20);
        BukkitTask task = new BukkitRunnable() {
            int remaining = totalTicks;

            @Override
            public void run() {
                if (remaining-- <= 0 || !player.isOnline() || player.isDead()) {
                    activeBleeds.remove(uuid);
                    cancel();
                    return;
                }
                bleedingPlayers.add(uuid);
                Vector velocity = player.getVelocity();
                player.damage(tickDamage, mob);
                player.setVelocity(velocity);
                bleedingPlayers.remove(uuid);
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 25, 0, true, false));
            }
        }.runTaskTimer(plugin, 20L, 20L);

        activeBleeds.put(uuid, task);
    }

    private void tryPoison(LivingEntity mob, Player player, ConfigurationSection roster) {
        double procChance = roster != null ? roster.getDouble("POISONOUS.proc-chance", 0.25) : 0.25;
        if (random.nextDouble() >= procChance) return;

        int durationTicks = roster != null ? roster.getInt("POISONOUS.duration-ticks", 100) : 100;
        int level = plugin.getMobScalingService().calculateLevel(mob);
        int amplifier = level >= 25 ? 1 : 0;
        player.addPotionEffect(new PotionEffect(PotionEffectType.POISON, durationTicks, amplifier));
    }

    private void tryChill(Player player, ConfigurationSection roster) {
        double procChance = roster != null ? roster.getDouble("CHILLING.proc-chance", 0.30) : 0.30;
        if (random.nextDouble() >= procChance) return;

        int durationTicks = roster != null ? roster.getInt("CHILLING.duration-ticks", 60) : 60;
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, durationTicks, 1));
    }
}
