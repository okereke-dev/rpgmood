package com.ricardo.rpgmood.farming.animal.listener;

import com.ricardo.rpgmood.RPGMoodPlugin;
import com.ricardo.rpgmood.farming.SeasonManager;
import com.ricardo.rpgmood.farming.animal.AnimalData;
import com.ricardo.rpgmood.farming.animal.AnimalManager;
import com.ricardo.rpgmood.farming.animal.AnimalType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs daily (every 24000 ticks = 20 min) to generate animal products.
 * Chickens lay eggs, cows/goats produce milk-ready status, sheep regrow wool.
 */
public class AnimalProductTask extends BukkitRunnable {

    private final RPGMoodPlugin plugin;
    private int tickCounter = 0;

    // 24000 ticks = 1 MC day (20 minutes real time)
    private static final int DAY_LENGTH_TICKS = 24000;

    public AnimalProductTask(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.getConfig().getBoolean("farming.animals.enabled", true)) return;

        tickCounter++;

        // Only process once per MC day
        if (tickCounter < DAY_LENGTH_TICKS) return;
        tickCounter = 0;

        AnimalManager manager = plugin.getAnimalManager();
        if (manager == null) return;

        // Run daily tick on the manager
        manager.dailyTick();

        // Process animal products
        for (AnimalData animal : manager.getAllAnimals().values()) {
            if (animal.isSick()) continue;
            if (!animal.canProduceToday(manager.getCurrentServerDay())) continue;
            if (animal.getLastFedDay() < manager.getCurrentServerDay() - 1) continue;

            Entity entity = findEntity(animal.getAnimalId());
            if (entity == null || entity.isDead()) continue;

            switch (animal.getType()) {
                case CHICKEN -> handleChickenEgg(manager, animal, entity);
                case COW, GOAT -> {
                    // Mark as producible (actual milking happens via interaction)
                    animal.setLastProductDay(manager.getCurrentServerDay());
                    manager.saveAnimal(animal);
                }
                case SHEEP -> {
                    // Sheep can be sheared again
                    animal.setLastProductDay(manager.getCurrentServerDay() - 4);
                    manager.saveAnimal(animal);
                }
            }
        }
    }

    /** Chickens lay eggs on the ground near them. */
    private void handleChickenEgg(AnimalManager manager, AnimalData animal, Entity entity) {
        AnimalManager.ProductQuality quality = manager.determineProductQuality(animal);
        int today = manager.getCurrentServerDay();

        ItemStack egg = new ItemStack(Material.EGG);
        ItemMeta meta = egg.getItemMeta();
        meta.setDisplayName("§f" + animal.getName() + "'s Egg");
        List<String> lore = new ArrayList<>();
        lore.add("§7Quality: " + quality.getDisplayName());
        meta.setLore(lore);
        egg.setItemMeta(meta);

        // Drop near the chicken
        entity.getWorld().dropItemNaturally(entity.getLocation(), egg);

        animal.setLastProductDay(today);
        manager.saveAnimal(animal);

        // Notify owner if online and nearby
        Player owner = Bukkit.getPlayer(animal.getOwnerId());
        if (owner != null && owner.isOnline()
                && owner.getWorld().equals(entity.getWorld())
                && owner.getLocation().distance(entity.getLocation()) < 50) {
            owner.sendActionBar(net.kyori.adventure.text.Component.text()
                    .append(net.kyori.adventure.text.Component.text("\uD83E\uDD5A ", net.kyori.adventure.text.format.NamedTextColor.YELLOW))
                    .append(net.kyori.adventure.text.Component.text(animal.getName() + " laid an egg! (" + quality.getDisplayName() + ")", net.kyori.adventure.text.format.NamedTextColor.WHITE))
                    .build());
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
