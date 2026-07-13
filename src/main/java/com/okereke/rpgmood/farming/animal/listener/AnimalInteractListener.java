package com.okereke.rpgmood.farming.animal.listener;

import com.okereke.rpgmood.RPGMoodPlugin;
import com.okereke.rpgmood.farming.animal.AnimalData;
import com.okereke.rpgmood.farming.animal.AnimalManager;
import com.okereke.rpgmood.farming.animal.AnimalType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityEnterLoveModeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles player interactions with owned animals:
 * - Petting (empty hand)
 * - Milking (bucket on cow/goat)
 * - Shearing (shears on sheep)
 * - Feeding (favorite food)
 * - Brushing (brush item)
 * - Info (stick)
 */
public class AnimalInteractListener implements Listener {

    private final RPGMoodPlugin plugin;

    public AnimalInteractListener(RPGMoodPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.isCancelled()) return;

        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();

        // Check if this entity is a registered animal
        AnimalManager manager = plugin.getAnimalManager();
        if (manager == null) return;

        AnimalData animal = manager.getAnimal(entity.getUniqueId());
        if (animal == null) {
            // Wild animal — claiming now happens via EntityEnterLoveModeEvent, the actual
            // vanilla trigger, instead of guessing from the item in hand here.
            return;
        }

        // Check ownership
        if (!animal.getOwnerId().equals(player.getUniqueId())) {
            plugin.getMessageService().send(player, Component.text("This isn't your animal!").color(NamedTextColor.RED));
            event.setCancelled(true);
            return;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        Material handType = hand.getType();

        // Determine interaction type
        if (handType == Material.AIR) {
            // Petting
            handlePet(player, animal, manager);
        } else if (handType == Material.BUCKET && (animal.getType() == AnimalType.COW || animal.getType() == AnimalType.GOAT)) {
            // Milking
            handleMilk(player, animal, manager);
            event.setCancelled(true);
        } else if (handType == Material.SHEARS && animal.getType() == AnimalType.SHEEP) {
            // Shearing
            handleShear(player, animal, manager, entity);
            event.setCancelled(true);
        } else if (handType == Material.BRUSH) {
            // Brushing
            handleBrush(player, animal, manager);
        } else if (isFavoriteFood(animal.getType(), handType)) {
            // Feeding
            handleFeed(player, animal, manager, hand);
            event.setCancelled(true);
        } else if (handType == Material.STICK) {
            // Info
            handleInfo(player, animal);
            event.setCancelled(true);
        }
    }

    // -- Befriending wild animals --

    /**
     * A wild animal becomes the feeder's the moment vanilla itself decides to show love-mode
     * hearts — {@link EntityEnterLoveModeEvent} only fires once vanilla's own eligibility
     * checks (adult, not on breeding cooldown, correct food) already passed, so this is the
     * exact vanilla trigger rather than a guess based on the item in the player's hand. Not
     * cancelled or altered in any way; breeding itself is untouched.
     */
    @EventHandler
    public void onEntityEnterLoveMode(EntityEnterLoveModeEvent event) {
        if (!(event.getHumanEntity() instanceof Player player)) return;

        LivingEntity entity = event.getEntity();
        AnimalManager manager = plugin.getAnimalManager();
        if (manager == null) return;
        if (manager.getAnimal(entity.getUniqueId()) != null) return; // already owned — this is breeding, not a claim

        AnimalType type = AnimalType.fromEntityType(entity.getType());
        if (type == null) return;

        if (!plugin.getConfig().getBoolean("farming.animals.enabled", true)) return;

        int maxPerPlayer = plugin.getConfig().getInt("farming.animals.max_per_player", 20);
        List<AnimalData> owned = manager.getOwnedAnimals(player.getUniqueId());
        if (owned.size() >= maxPerPlayer) {
            plugin.getMessageService().send(player, Component.text("You already have the maximum number of animals (" + maxPerPlayer + ").").color(NamedTextColor.RED));
            return;
        }

        entity.setPersistent(true);
        String name = type.getDisplayName() + " #" + (owned.size() + 1);
        manager.registerAnimal(entity.getUniqueId(), type, player.getUniqueId(), name);
        plugin.getAchievementManager().onAnimalClaimed(player, type);

        plugin.getMessageService().send(player, Component.text()
                .append(Component.text("🐾 The " + type.getDisplayName() + " trusts you", NamedTextColor.GREEN))
                .append(Component.text(" — it's yours now!", NamedTextColor.GREEN))
                .build());
        plugin.getPlayerJournalService().addEntry(player, "Befriended a wild " + type.getDisplayName() + "!");
    }

    // -- Petting --

    private void handlePet(Player player, AnimalData animal, AnimalManager manager) {
        int today = manager.getCurrentServerDay();
        if (animal.getLastInteractionDay() == today) {
            plugin.getMessageService().send(player, Component.text(animal.getName() + " has been petted today already.")
                    .color(NamedTextColor.YELLOW));
            return;
        }

        animal.setLastInteractionDay(today);
        animal.setAffection(animal.getAffection() + 1.0);
        manager.saveAnimal(animal);
        if (animal.getHeartLevel() >= 5) {
            plugin.getAchievementManager().onAnimalMaxAffection(player);
        }

        plugin.getMessageService().send(player, Component.text()
                .append(Component.text("\uD83D\uDC4B You pet ", NamedTextColor.GREEN))
                .append(Component.text(animal.getName(), NamedTextColor.WHITE))
                .append(Component.text(". Affection +1", NamedTextColor.GRAY))
                .build());

        int hearts = animal.getHeartLevel();
        if (hearts > 0) {
            plugin.getMessageService().send(player, Component.text("❤️".repeat(hearts) + "♡".repeat(5 - hearts))
                    .color(NamedTextColor.RED));
        }
    }

    // -- Brushing --

    private void handleBrush(Player player, AnimalData animal, AnimalManager manager) {
        int today = manager.getCurrentServerDay();
        if (animal.getLastInteractionDay() == today) {
            plugin.getMessageService().send(player, Component.text(animal.getName() + " is already clean today.")
                    .color(NamedTextColor.YELLOW));
            return;
        }

        // Brushing counts as interaction and gives more affection
        animal.setLastInteractionDay(today);
        animal.setAffection(animal.getAffection() + 2.0);
        manager.saveAnimal(animal);
        if (animal.getHeartLevel() >= 5) {
            plugin.getAchievementManager().onAnimalMaxAffection(player);
        }

        plugin.getMessageService().send(player, Component.text()
                .append(Component.text("\uD83E\uDDFB Brushed ", NamedTextColor.AQUA))
                .append(Component.text(animal.getName(), NamedTextColor.WHITE))
                .append(Component.text(". Affection +2!", NamedTextColor.GRAY))
                .build());
    }

    // -- Milking --

    private void handleMilk(Player player, AnimalData animal, AnimalManager manager) {
        int today = manager.getCurrentServerDay();

        if (!animal.canProduceToday(today)) {
            plugin.getMessageService().send(player, Component.text(animal.getName() + " has already been milked today.")
                    .color(NamedTextColor.YELLOW));
            return;
        }

        if (animal.isSick()) {
            plugin.getMessageService().send(player, Component.text(animal.getName() + " is sick and can't be milked.")
                    .color(NamedTextColor.RED));
            return;
        }

        if (animal.getLastFedDay() < today - 1) {
            plugin.getMessageService().send(player, Component.text(animal.getName() + " needs to eat first!")
                    .color(NamedTextColor.RED));
            return;
        }

        // Determine quality
        AnimalManager.ProductQuality quality = manager.determineProductQuality(animal);

        // Consume one bucket
        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getAmount() > 1) {
            handItem.setAmount(handItem.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        ItemStack milk = new ItemStack(Material.MILK_BUCKET);
        ItemMeta meta = milk.getItemMeta();
        meta.setDisplayName("§f" + animal.getName() + "'s Milk");
        List<String> lore = new ArrayList<>();
        lore.add("§7Quality: " + quality.getDisplayName());
        lore.add("§7Affection: " + getHeartsDisplay(animal));
        meta.setLore(lore);
        milk.setItemMeta(meta);

        // Try to add to inventory or drop
        if (player.getInventory().addItem(milk).isEmpty()) {
            plugin.getMessageService().send(player, Component.text()
                    .append(Component.text("\uD83E\uDD5C Milked ", NamedTextColor.WHITE))
                    .append(Component.text(animal.getName(), NamedTextColor.WHITE))
                    .append(Component.text("! Quality: ", NamedTextColor.GRAY))
                    .append(Component.text(quality.getDisplayName()))
                    .build());
        }

        animal.setLastProductDay(today);

        // Affection bonus for milking
        double currentAff = animal.getAffection();
        if (currentAff < 10.0) {
            animal.setAffection(currentAff + 0.5);
        }

        manager.saveAnimal(animal);
        if (animal.getHeartLevel() >= 5) {
            plugin.getAchievementManager().onAnimalMaxAffection(player);
        }

        plugin.getPlayerJournalService().addEntry(player,
                "Milked " + animal.getName() + " (" + quality.getDisplayName() + " quality)");
    }

    // -- Shearing --

    private void handleShear(Player player, AnimalData animal, AnimalManager manager, Entity entity) {
        int today = manager.getCurrentServerDay();

        if (!animal.canProduceToday(today)) {
            plugin.getMessageService().send(player, Component.text(animal.getName() + " has no wool yet. Wait 3 days.")
                    .color(NamedTextColor.YELLOW));
            return;
        }

        if (animal.isSick()) {
            plugin.getMessageService().send(player, Component.text(animal.getName() + " is sick and has poor wool.")
                    .color(NamedTextColor.RED));
            return;
        }

        // Determine quality and quantity
        AnimalManager.ProductQuality quality = manager.determineProductQuality(animal);
        int quantity = (int) Math.max(1, quality.getBonusMultiplier());

        ItemStack wool = new ItemStack(Material.WHITE_WOOL, quantity);
        ItemMeta meta = wool.getItemMeta();
        meta.setDisplayName("§f" + animal.getName() + "'s Wool");
        List<String> lore = new ArrayList<>();
        lore.add("§7Quality: " + quality.getDisplayName());
        meta.setLore(lore);
        wool.setItemMeta(meta);

        // Drop wool instead of adding to inventory (shearing drops on ground)
        entity.getWorld().dropItemNaturally(entity.getLocation(), wool);

        // Damage shears
        player.getItemInHand().damage(1, player);

        animal.setLastProductDay(today);

        double currentAff = animal.getAffection();
        if (currentAff < 10.0) {
            animal.setAffection(currentAff + 0.5);
        }

        manager.saveAnimal(animal);
        if (animal.getHeartLevel() >= 5) {
            plugin.getAchievementManager().onAnimalMaxAffection(player);
        }

        plugin.getMessageService().send(player, Component.text()
                .append(Component.text("\uD83D\uDC11 Sheared ", NamedTextColor.WHITE))
                .append(Component.text(animal.getName(), NamedTextColor.WHITE))
                .append(Component.text("! Got " + quantity + "x Wool (" + quality.getDisplayName() + ")", NamedTextColor.GRAY))
                .build());
    }

    // -- Feeding --

    private void handleFeed(Player player, AnimalData animal, AnimalManager manager, ItemStack hand) {
        int today = manager.getCurrentServerDay();

        if (animal.isFedToday(today)) {
            plugin.getMessageService().send(player, Component.text(animal.getName() + " is already full today.")
                    .color(NamedTextColor.YELLOW));
            return;
        }

        // Consume one item
        hand.setAmount(hand.getAmount() - 1);

        animal.setLastFedDay(today);
        animal.setHunger(Math.min(1.0, animal.getHunger() + 0.4));

        double currentAff = animal.getAffection();
        if (currentAff < 10.0) {
            animal.setAffection(Math.min(10.0, currentAff + 3.0));
        }

        manager.saveAnimal(animal);
        if (animal.getHeartLevel() >= 5) {
            plugin.getAchievementManager().onAnimalMaxAffection(player);
        }

        plugin.getMessageService().send(player, Component.text()
                .append(Component.text("\uD83C\uDF3E Fed ", NamedTextColor.GREEN))
                .append(Component.text(animal.getName(), NamedTextColor.WHITE))
                .append(Component.text(". Affection +3!", NamedTextColor.GRAY))
                .build());
    }

    // -- Info Display --

    private void handleInfo(Player player, AnimalData animal) {
        int hearts = animal.getHeartLevel();

        player.sendMessage(Component.text("=== " + animal.getName() + " ===").color(NamedTextColor.GOLD));
        player.sendMessage(Component.text("Type: ").color(NamedTextColor.GRAY)
                .append(Component.text(animal.getType().getDisplayName(), NamedTextColor.WHITE)));
        player.sendMessage(Component.text("Affection: ").color(NamedTextColor.GRAY)
                .append(Component.text("❤️".repeat(hearts) + "♡".repeat(5 - hearts), NamedTextColor.RED))
                .append(Component.text(" (" + String.format("%.1f", animal.getAffection()) + "/10)", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("Health: ").color(NamedTextColor.GRAY)
                .append(Component.text(getHealthBar(animal.getHealth()), getHealthColor(animal.getHealth()))));
        player.sendMessage(Component.text("Hunger: ").color(NamedTextColor.GRAY)
                .append(Component.text(getHungerBar(animal.getHunger()), getHungerColor(animal.getHunger()))));
        player.sendMessage(Component.text("Status: ").color(NamedTextColor.GRAY)
                .append(Component.text(animal.isSick() ? "§cSick" : "§aHealthy")));
        player.sendMessage(Component.text("Location: ").color(NamedTextColor.GRAY)
                .append(Component.text(animal.isInside() ? "§aInside" : "§eOutside")));
    }

    // -- Helpers --

    private boolean isFavoriteFood(AnimalType type, Material material) {
        return type.getFavoriteFood() == material
                || material == Material.WHEAT       // universal favorite
                || material == Material.HAY_BLOCK;  // universal winter food
    }

    private String getHeartsDisplay(AnimalData animal) {
        int hearts = animal.getHeartLevel();
        return "❤️".repeat(hearts) + "♡".repeat(5 - hearts);
    }

    private String getHealthBar(double health) {
        int bars = (int) Math.round(health * 10);
        return "█".repeat(Math.max(0, bars)) + "░".repeat(Math.max(0, 10 - bars));
    }

    private NamedTextColor getHealthColor(double health) {
        if (health > 0.6) return NamedTextColor.GREEN;
        if (health > 0.3) return NamedTextColor.YELLOW;
        return NamedTextColor.RED;
    }

    private String getHungerBar(double hunger) {
        int bars = (int) Math.round(hunger * 10);
        return "█".repeat(Math.max(0, bars)) + "░".repeat(Math.max(0, 10 - bars));
    }

    private NamedTextColor getHungerColor(double hunger) {
        if (hunger > 0.6) return NamedTextColor.GREEN;
        if (hunger > 0.3) return NamedTextColor.YELLOW;
        return NamedTextColor.RED;
    }
}
