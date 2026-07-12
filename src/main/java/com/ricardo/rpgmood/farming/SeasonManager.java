package com.ricardo.rpgmood.farming;

import com.ricardo.rpgmood.RPGMoodPlugin;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

/**
 * Manages a 4-season cycle (Spring → Summer → Autumn → Winter).
 * Each season lasts a configurable number of Minecraft days (default 30).
 * Influences crop growth, mob behaviour, and ambient events.
 */
public class SeasonManager {

    public enum Season {
        SPRING("🌸", "Spring", "A time of renewal and planting."),
        SUMMER("☀️", "Summer", "The sun fuels rapid growth."),
        AUTUMN("🍂", "Autumn", "Harvest season approaches."),
        WINTER("❄️", "Winter", "The cold slows all but the hardy.");

        private final String icon;
        private final String displayName;
        private final String description;

        Season(String icon, String displayName, String description) {
            this.icon = icon;
            this.displayName = displayName;
            this.description = description;
        }

        public String getIcon() { return icon; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }

        public Season next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private static final String SEASON_PATH = "season.current";
    private static final String DAY_PATH = "season.day";

    private final RPGMoodPlugin plugin;
    private final File dataFile;
    private final FileConfiguration data;
    private Season currentSeason;
    private int dayInSeason;

    public SeasonManager(RPGMoodPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "season.yml");
        this.data = YamlConfiguration.loadConfiguration(dataFile);

        String savedSeason = data.getString(SEASON_PATH, "SPRING");
        this.currentSeason = Season.valueOf(savedSeason.toUpperCase());
        this.dayInSeason = data.getInt(DAY_PATH, 0);
    }

    /** Advances the season by one Minecraft day. Called periodically from a task. */
    public void advanceDay() {
        int seasonLength = plugin.getConfig().getInt("farming.season_length_days", 30);
        dayInSeason++;

        if (dayInSeason >= seasonLength) {
            dayInSeason = 0;
            currentSeason = currentSeason.next();
            Bukkit.broadcastMessage("§6[Seasons] " + currentSeason.getIcon()
                    + " §e" + currentSeason.getDisplayName() + " has arrived! "
                    + currentSeason.getDescription());
        }

        data.set(SEASON_PATH, currentSeason.name());
        data.set(DAY_PATH, dayInSeason);
        try { data.save(dataFile); } catch (Exception ignored) {}

        plugin.getAchievementManager().onGlobalDayPassed();
    }

    public Season getCurrentSeason() { return currentSeason; }
    public int getDayInSeason() { return dayInSeason; }

    /** Returns the growth multiplier for a given season (Spring=1.5, Summer=2.0, Autumn=1.0, Winter=0.3). */
    public double getGrowthMultiplier() {
        return switch (currentSeason) {
            case SPRING -> 1.5;
            case SUMMER -> 2.0;
            case AUTUMN -> 1.0;
            case WINTER -> 0.3;
        };
    }

    /** Returns which crops are in season for the current season from farming.yml. */
    public List<String> getSeasonalCrops() {
        return plugin.getConfig().getStringList("farming.seasons." + currentSeason.name().toLowerCase() + ".crops");
    }
}
