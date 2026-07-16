package com.okereke.rpgmood;

import org.bukkit.entity.Warden;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Scales Warden sonic-boom damage. Vanilla sonic boom ignores {@code attack_damage} and deals
 * a fixed amount; without this listener a high-level Warden's ranged attack stays at vanilla
 * strength while its melee/HP follow the scaling curve.
 */
public class BossDamageListener implements Listener {

    private final RPGMoodPlugin plugin;

    public BossDamageListener(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSonicBoom(EntityDamageByEntityEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.SONIC_BOOM) {
            return;
        }
        if (!(event.getDamager() instanceof Warden warden)) {
            return;
        }
        if (!warden.getScoreboardTags().contains("rpgmood_scaled")) {
            return;
        }

        double multiplier = plugin.getMobScalingService().resolveStatMultiplier(warden);
        if (multiplier == 1.0) {
            return;
        }
        event.setDamage(event.getDamage() * multiplier);
    }
}
