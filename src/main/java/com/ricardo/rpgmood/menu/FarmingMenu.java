package com.ricardo.rpgmood.menu;

import com.ricardo.rpgmood.RPGMoodPlugin;
import com.ricardo.rpgmood.farming.CropManager;
import com.ricardo.rpgmood.farming.SeasonManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

import java.util.List;

/** Current season + this season's crops — visual equivalent of /rpgmood-farm season and crops. */
public class FarmingMenu implements RPGMoodMenu {

    private static final int SLOT_BACK = 0;
    private static final int SLOT_SEASON = 4;
    private static final int[] CROP_SLOTS = {19, 20, 21, 22, 23, 24, 25};

    private final RPGMoodPlugin plugin;
    private final Inventory inventory;

    public FarmingMenu(RPGMoodPlugin plugin) {
        this.plugin = plugin;
        this.inventory = Bukkit.createInventory(this, 36, Component.text("✦ RPGMood — Farming", NamedTextColor.GOLD));
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void render() {
        MenuUtil.fillBorder(inventory);
        inventory.setItem(SLOT_BACK, MenuUtil.icon(Material.ARROW, Component.text("← Back", NamedTextColor.GRAY)));

        SeasonManager sm = plugin.getSeasonManager();
        SeasonManager.Season season = sm.getCurrentSeason();
        int day = sm.getDayInSeason();
        int seasonLength = plugin.getConfig().getInt("farming.season_length_days", 30);

        inventory.setItem(SLOT_SEASON, MenuUtil.icon(Material.CLOCK,
                Component.text(season.getIcon() + " " + season.getDisplayName(), NamedTextColor.YELLOW),
                List.of(
                        Component.text("Day " + (day + 1) + " of " + seasonLength, NamedTextColor.GRAY),
                        Component.text("Growth x" + String.format("%.1f", sm.getGrowthMultiplier()), NamedTextColor.GREEN),
                        Component.empty(),
                        Component.text(season.getDescription(), NamedTextColor.DARK_GRAY)
                )));

        for (int slot : CROP_SLOTS) inventory.setItem(slot, null);
        List<String> seasonalCrops = sm.getSeasonalCrops();
        for (int i = 0; i < seasonalCrops.size() && i < CROP_SLOTS.length; i++) {
            String cropKey = seasonalCrops.get(i);
            CropManager.CropDefinition def = plugin.getCropManager().getCropDefinitions().get(cropKey);
            Material material = def != null ? def.material() : Material.WHEAT;
            String label = Character.toUpperCase(cropKey.charAt(0)) + cropKey.substring(1);
            inventory.setItem(CROP_SLOTS[i], MenuUtil.icon(material, Component.text(label, NamedTextColor.WHITE)));
        }
        if (seasonalCrops.isEmpty()) {
            inventory.setItem(CROP_SLOTS[0], MenuUtil.icon(Material.BARRIER, Component.text("No crops this season", NamedTextColor.GRAY)));
        }
    }

    @Override
    public void handleClick(Player player, int slot) {
        if (slot == SLOT_BACK) {
            player.openInventory(new MainMenu(plugin, player).getInventory());
        }
    }
}
