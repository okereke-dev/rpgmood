package com.ricardo.rpgmood;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.entity.LivingEntity;

public class MobSpawnListener implements Listener {

    private final RPGMoodPlugin plugin;

    public MobSpawnListener(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMobSpawn(CreatureSpawnEvent event) {
        if (event == null) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (entity == null || event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.CUSTOM) {
            return;
        }
        if (plugin.getMobScalingService() == null) {
            return;
        }
        plugin.getMobScalingService().applyScaling(entity);
    }
}
