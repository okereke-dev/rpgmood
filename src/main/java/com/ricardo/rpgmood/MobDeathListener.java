package com.ricardo.rpgmood;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataType;

public class MobDeathListener implements Listener {

    private final RPGMoodPlugin plugin;

    public MobDeathListener(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
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
    }
}
