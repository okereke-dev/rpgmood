package com.okereke.rpgmood.menu;

import com.okereke.rpgmood.RPGMoodPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

/**
 * In-game editor for the handful of numeric/boolean tunables players have asked to adjust
 * without editing YAML by hand — not a generic config editor (that's a much bigger project),
 * just steppers for the values that came up this session: spawn protection radius, the mob
 * scaling curve, night/thunder bonuses, and the weather-effects toggle. Writes straight
 * through {@code plugin.getConfig()} + {@code saveConfig()}, same as any other admin change.
 */
public class AdminConfigMenu implements RPGMoodMenu {

    private static final int SLOT_BACK = 0;

    private static final int SLOT_RADIUS_MINUS = 9, SLOT_RADIUS_VALUE = 10, SLOT_RADIUS_PLUS = 11;
    private static final int SLOT_EARLY_MINUS = 12, SLOT_EARLY_VALUE = 13, SLOT_EARLY_PLUS = 14;
    private static final int SLOT_PARITY_MINUS = 15, SLOT_PARITY_VALUE = 16, SLOT_PARITY_PLUS = 17;
    private static final int SLOT_NIGHT_MINUS = 18, SLOT_NIGHT_VALUE = 19, SLOT_NIGHT_PLUS = 20;
    private static final int SLOT_THUNDER_MINUS = 21, SLOT_THUNDER_VALUE = 22, SLOT_THUNDER_PLUS = 23;
    private static final int SLOT_WEATHER_TOGGLE = 25;

    private final RPGMoodPlugin plugin;
    private final Inventory inventory;

    public AdminConfigMenu(RPGMoodPlugin plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 36, Component.text("✦ RPGMood — Admin Config", NamedTextColor.GOLD));
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void render() {
        MenuUtil.fillBorder(inventory);
        inventory.setItem(SLOT_BACK, MenuUtil.icon(Material.ARROW, Component.text("← Back", NamedTextColor.GRAY)));

        renderStepper(SLOT_RADIUS_MINUS, SLOT_RADIUS_VALUE, SLOT_RADIUS_PLUS, Material.OAK_FENCE,
                "Spawn Protection Radius", plugin.getConfig().getInt("spawn_protection.radius", 64) + " blocks");
        renderStepper(SLOT_EARLY_MINUS, SLOT_EARLY_VALUE, SLOT_EARLY_PLUS, Material.LEATHER_CHESTPLATE,
                "Early Game Fraction", String.format("%.2f", plugin.getConfig().getDouble("mob_scaling.early_game_fraction", 0.85)));
        renderStepper(SLOT_PARITY_MINUS, SLOT_PARITY_VALUE, SLOT_PARITY_PLUS, Material.IRON_CHESTPLATE,
                "Parity Level", String.valueOf(plugin.getConfig().getInt("mob_scaling.parity_level", 8)));
        renderStepper(SLOT_NIGHT_MINUS, SLOT_NIGHT_VALUE, SLOT_NIGHT_PLUS, Material.BLACK_DYE,
                "Night Bonus", "+" + plugin.getConfig().getInt("mob_scaling.night_bonus", 2) + " levels");
        renderStepper(SLOT_THUNDER_MINUS, SLOT_THUNDER_VALUE, SLOT_THUNDER_PLUS, Material.TRIDENT,
                "Thunder Bonus", "+" + plugin.getConfig().getInt("mob_scaling.thunder_bonus", 2) + " levels");

        boolean weatherEnabled = plugin.getConfig().getBoolean("weather_effects.enabled", true);
        inventory.setItem(SLOT_WEATHER_TOGGLE, MenuUtil.icon(
                weatherEnabled ? Material.LIME_DYE : Material.GRAY_DYE,
                Component.text((weatherEnabled ? "✅ " : "❌ ") + "Weather Effects", weatherEnabled ? NamedTextColor.GREEN : NamedTextColor.GRAY),
                List.of(Component.text("Fog + wind during storms.", NamedTextColor.DARK_GRAY),
                        Component.text("Click to " + (weatherEnabled ? "disable" : "enable"), NamedTextColor.YELLOW))));
    }

    private void renderStepper(int minusSlot, int valueSlot, int plusSlot, Material material, String label, String value) {
        inventory.setItem(minusSlot, MenuUtil.icon(Material.RED_STAINED_GLASS_PANE, Component.text("−", NamedTextColor.RED)));
        inventory.setItem(valueSlot, MenuUtil.icon(material,
                Component.text(label, NamedTextColor.YELLOW),
                List.of(Component.text(value, NamedTextColor.WHITE))));
        inventory.setItem(plusSlot, MenuUtil.icon(Material.LIME_STAINED_GLASS_PANE, Component.text("+", NamedTextColor.GREEN)));
    }

    @Override
    public void handleClick(Player player, int slot) {
        switch (slot) {
            case SLOT_BACK -> player.openInventory(new MainMenu(plugin, player).getInventory());
            case SLOT_RADIUS_MINUS -> adjustInt("spawn_protection.radius", -8, 0, Integer.MAX_VALUE);
            case SLOT_RADIUS_PLUS -> adjustInt("spawn_protection.radius", 8, 0, Integer.MAX_VALUE);
            case SLOT_EARLY_MINUS -> adjustDouble("mob_scaling.early_game_fraction", -0.05, 0.1, 1.0);
            case SLOT_EARLY_PLUS -> adjustDouble("mob_scaling.early_game_fraction", 0.05, 0.1, 1.0);
            case SLOT_PARITY_MINUS -> adjustInt("mob_scaling.parity_level", -1, 1, Integer.MAX_VALUE);
            case SLOT_PARITY_PLUS -> adjustInt("mob_scaling.parity_level", 1, 1, Integer.MAX_VALUE);
            case SLOT_NIGHT_MINUS -> adjustInt("mob_scaling.night_bonus", -1, 0, Integer.MAX_VALUE);
            case SLOT_NIGHT_PLUS -> adjustInt("mob_scaling.night_bonus", 1, 0, Integer.MAX_VALUE);
            case SLOT_THUNDER_MINUS -> adjustInt("mob_scaling.thunder_bonus", -1, 0, Integer.MAX_VALUE);
            case SLOT_THUNDER_PLUS -> adjustInt("mob_scaling.thunder_bonus", 1, 0, Integer.MAX_VALUE);
            case SLOT_WEATHER_TOGGLE -> {
                boolean enabled = !plugin.getConfig().getBoolean("weather_effects.enabled", true);
                plugin.getConfig().set("weather_effects.enabled", enabled);
                plugin.saveConfig();
            }
            default -> { return; }
        }
        render();
    }

    private void adjustInt(String path, int delta, int min, int max) {
        int value = Math.max(min, Math.min(max, plugin.getConfig().getInt(path) + delta));
        plugin.getConfig().set(path, value);
        plugin.saveConfig();
    }

    private void adjustDouble(String path, double delta, double min, double max) {
        double value = Math.max(min, Math.min(max, plugin.getConfig().getDouble(path) + delta));
        plugin.getConfig().set(path, Math.round(value * 100.0) / 100.0);
        plugin.saveConfig();
    }
}
