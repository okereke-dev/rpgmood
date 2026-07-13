package com.okereke.rpgmood.farming.animal.listener;

import com.okereke.rpgmood.RPGMoodPlugin;
import com.okereke.rpgmood.farming.animal.AnimalData;
import com.okereke.rpgmood.farming.animal.AnimalManager;
import com.okereke.rpgmood.farming.animal.AnimalType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;

import java.util.List;
import java.util.UUID;

/**
 * Vanilla breeding grows the herd: if a newborn's parents share the same owner (or one parent
 * is owned and the other is wild), the baby is automatically claimed by that owner. Parents
 * with different owners, or two wild parents, leave the baby unclaimed — same as vanilla.
 */
public class AnimalBreedListener implements Listener {

    private final RPGMoodPlugin plugin;

    public AnimalBreedListener(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBreed(EntityBreedEvent event) {
        AnimalType type = AnimalType.fromEntityType(event.getEntity().getType());
        if (type == null) return;

        AnimalManager manager = plugin.getAnimalManager();
        if (manager == null || !plugin.getConfig().getBoolean("farming.animals.enabled", true)) return;

        UUID owner = resolveOwner(manager, event.getMother(), event.getFather());
        if (owner == null) return;

        Player player = Bukkit.getPlayer(owner);
        int maxPerPlayer = plugin.getConfig().getInt("farming.animals.max_per_player", 20);
        List<AnimalData> owned = manager.getOwnedAnimals(owner);
        if (owned.size() >= maxPerPlayer) return;

        LivingEntity baby = event.getEntity();
        baby.setPersistent(true);
        String name = type.getDisplayName() + " #" + (owned.size() + 1);
        manager.registerAnimal(baby.getUniqueId(), type, owner, name);

        if (player != null && player.isOnline()) {
            plugin.getAchievementManager().onAnimalClaimed(player, type);
            plugin.getMessageService().send(player, Component.text()
                    .append(Component.text("🐣 A new " + type.getDisplayName() + " was born ", NamedTextColor.GREEN))
                    .append(Component.text("on your farm!", NamedTextColor.GREEN))
                    .build());
            plugin.getPlayerJournalService().addEntry(player, "A new " + type.getDisplayName() + " was born on your farm!");
        }
    }

    /** Same owner on both parents wins; one owned + one wild adopts the owned parent's owner; anything else stays unclaimed. */
    private UUID resolveOwner(AnimalManager manager, LivingEntity mother, LivingEntity father) {
        AnimalData motherData = manager.getAnimal(mother.getUniqueId());
        AnimalData fatherData = manager.getAnimal(father.getUniqueId());
        UUID motherOwner = motherData != null ? motherData.getOwnerId() : null;
        UUID fatherOwner = fatherData != null ? fatherData.getOwnerId() : null;

        if (motherOwner != null && motherOwner.equals(fatherOwner)) return motherOwner;
        if (motherOwner != null && fatherOwner == null) return motherOwner;
        if (fatherOwner != null && motherOwner == null) return fatherOwner;
        return null;
    }
}
