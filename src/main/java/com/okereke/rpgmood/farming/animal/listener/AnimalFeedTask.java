package com.okereke.rpgmood.farming.animal.listener;

import com.okereke.rpgmood.RPGMoodPlugin;
import com.okereke.rpgmood.farming.animal.AnimalData;
import com.okereke.rpgmood.farming.animal.AnimalManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Periodic task that checks if animals are grazing on grass.
 * Runs every 1200 ticks (60s) and marks animals as fed if they graze for 5+ minutes.
 */
public class AnimalFeedTask extends BukkitRunnable {

    private static final long GRAZING_TICKS_REQUIRED = 100; // ~5 minutes of grazing to count as fed
    private static final long CHECK_INTERVAL = 1200; // ticks (60s)

    private final RPGMoodPlugin plugin;
    private long grazingTicks = 0;

    public AnimalFeedTask(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("farming.animals.enabled", true)) return;

        AnimalManager manager = plugin.getAnimalManager();
        if (manager == null) return;

        grazingTicks += CHECK_INTERVAL;

        // Check every ~60 seconds if enough time has passed
        if (grazingTicks < GRAZING_TICKS_REQUIRED) return;
        grazingTicks = 0;

        int today = manager.getCurrentServerDay();

        for (AnimalData animal : manager.getAllAnimals().values()) {
            // Skip if already fed today
            if (animal.isFedToday(today)) continue;

            Entity entity = findEntity(animal.getAnimalId());
            if (entity == null || entity.isDead()) continue;

            // Check grazing
            boolean grazing = manager.isGrazing(animal.getAnimalId());

            // Check hay feeder
            boolean hayNearby = manager.hasHayNearby(animal.getAnimalId());

            if (grazing || hayNearby) {
                animal.setLastFedDay(today);
                animal.setHunger(Math.min(1.0, animal.getHunger() + 0.3));

                // Small affection bonus for grazing outdoors
                if (grazing && animal.getAffection() < 10.0) {
                    animal.setAffection(animal.getAffection() + 1.0);
                }

                manager.saveAnimal(animal);
            }
        }
    }

    private Entity findEntity(java.util.UUID uuid) {
        for (org.bukkit.World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity.getUniqueId().equals(uuid)) {
                    return entity;
                }
            }
        }
        return null;
    }
}
