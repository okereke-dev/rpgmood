package com.ricardo.rpgmood.menu;

import com.ricardo.rpgmood.RPGMoodPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

/** Player preference toggles — the same three the /rpgmood toggle command and MessageService already support, just clickable. */
public class SettingsMenu implements RPGMoodMenu {

    private static final int SLOT_EFFECTS = 11;
    private static final int SLOT_TITLES = 13;
    private static final int SLOT_DELIVERY = 15;
    private static final int SLOT_BACK = 22;

    private final RPGMoodPlugin plugin;
    private final Inventory inventory;

    public SettingsMenu(RPGMoodPlugin plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 27, Component.text("✦ RPGMood — Settings", NamedTextColor.GOLD));
        render();
    }

    @Override
    public Inventory getInventory() { return inventory; }

    private void render() {
        MenuUtil.fillBorder(inventory);
        inventory.setItem(SLOT_BACK, MenuUtil.icon(Material.ARROW, Component.text("← Back", NamedTextColor.GRAY)));
    }

    /** Toggle state is per-player, so the icons are rendered right before the menu is shown to a specific player. */
    public void renderFor(Player player) {
        boolean effects = getToggle(player, "player_effects.", true);
        boolean titles = getToggle(player, "player_titles.", true);
        boolean actionBar = getToggle(player, "player_actionbar.", true);

        inventory.setItem(SLOT_EFFECTS, toggleIcon("Ambient Effects", "Zone titles, sounds, and ambient messages", effects));
        inventory.setItem(SLOT_TITLES, toggleIcon("Zone Titles", "The big on-screen title when entering a zone", titles));
        inventory.setItem(SLOT_DELIVERY, toggleIcon("Action Bar Delivery", "On: messages show above your hotbar. Off: sent to chat instead.", actionBar));
    }

    private org.bukkit.inventory.ItemStack toggleIcon(String name, String description, boolean enabled) {
        Material material = enabled ? Material.LIME_DYE : Material.GRAY_DYE;
        return MenuUtil.icon(material, Component.text((enabled ? "✅ " : "❌ ") + name, enabled ? NamedTextColor.GREEN : NamedTextColor.GRAY),
                List.of(
                        Component.text(description, NamedTextColor.DARK_GRAY),
                        Component.empty(),
                        Component.text("Click to " + (enabled ? "disable" : "enable"), NamedTextColor.YELLOW)
                ));
    }

    private boolean getToggle(Player player, String key, boolean defaultValue) {
        return plugin.getConfigManager().getConfigValues().getBoolean(key + player.getUniqueId(), defaultValue);
    }

    @Override
    public void handleClick(Player player, int slot) {
        switch (slot) {
            case SLOT_EFFECTS -> flip(player, "player_effects.");
            case SLOT_TITLES -> flip(player, "player_titles.");
            case SLOT_DELIVERY -> flip(player, "player_actionbar.");
            case SLOT_BACK -> player.openInventory(new MainMenu(plugin, player).getInventory());
            default -> { }
        }
    }

    private void flip(Player player, String key) {
        boolean newValue = !getToggle(player, key, true);
        plugin.getConfigManager().savePlayerToggle(key, player.getUniqueId(), newValue);
        renderFor(player);
    }
}
