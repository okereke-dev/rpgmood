package com.okereke.rpgmood.menu;

import com.okereke.rpgmood.RPGMoodPlugin;
import com.okereke.rpgmood.farming.animal.AnimalData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

/** Visual list of the viewer's own animals — equivalent of /rpgmood-farm animal list. */
public class AnimalsMenu implements RPGMoodMenu {

    private static final int SLOT_BACK = 0;
    private static final int[] ANIMAL_SLOTS = {
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26
    };

    private final RPGMoodPlugin plugin;
    private final Inventory inventory;

    public AnimalsMenu(RPGMoodPlugin plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 36, Component.text("✦ RPGMood — My Animals", NamedTextColor.GOLD));
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void renderFor(Player player) {
        MenuUtil.fillBorder(inventory);
        inventory.setItem(SLOT_BACK, MenuUtil.icon(Material.ARROW, Component.text("← Back", NamedTextColor.GRAY)));

        List<AnimalData> animals = plugin.getAnimalManager().getOwnedAnimals(player.getUniqueId());
        for (int slot : ANIMAL_SLOTS) inventory.setItem(slot, null);

        if (animals.isEmpty()) {
            inventory.setItem(ANIMAL_SLOTS[0], MenuUtil.icon(Material.HAY_BLOCK,
                    Component.text("No animals yet", NamedTextColor.GRAY),
                    List.of(Component.text("Feed a wild cow/chicken/sheep/goat its", NamedTextColor.DARK_GRAY),
                            Component.text("favorite food to befriend it!", NamedTextColor.DARK_GRAY))));
            return;
        }

        for (int i = 0; i < animals.size() && i < ANIMAL_SLOTS.length; i++) {
            AnimalData animal = animals.get(i);
            int hearts = animal.getHeartLevel();
            String heartDisplay = "❤️".repeat(hearts) + "♡".repeat(5 - hearts);
            Material egg = spawnEggFor(animal.getType().name());

            inventory.setItem(ANIMAL_SLOTS[i], MenuUtil.icon(egg,
                    Component.text(animal.getName(), NamedTextColor.WHITE),
                    List.of(
                            Component.text(animal.getType().getDisplayName(), NamedTextColor.GRAY),
                            Component.text(heartDisplay, NamedTextColor.RED),
                            Component.text(animal.isSick() ? "☠ Sick" : "✔ Healthy", animal.isSick() ? NamedTextColor.RED : NamedTextColor.GREEN)
                    )));
        }
    }

    private static Material spawnEggFor(String animalTypeName) {
        try {
            return Material.valueOf(animalTypeName + "_SPAWN_EGG");
        } catch (IllegalArgumentException e) {
            return Material.EGG;
        }
    }

    @Override
    public void handleClick(Player player, int slot) {
        if (slot == SLOT_BACK) {
            player.openInventory(new MainMenu(plugin, player).getInventory());
        }
    }
}
