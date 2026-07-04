package com.ricardo.rpgmood;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public class RPGMoodPlugin extends JavaPlugin {

    /** bStats plugin ID — 0 until RPGMood is registered at bstats.org/what-is-my-plugin-id; metrics are skipped until then. */
    private static final int BSTATS_PLUGIN_ID = 0;

    private static RPGMoodPlugin instance;
    private ZoneManager zoneManager;
    private ConfigManager configManager;
    private MobScalingService mobScalingService;
    private PlayerJournalService playerJournalService;
    private PlayerStatsService playerStatsService;
    private boolean worldGuardActive;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardActive = true;
            getLogger().info("WorldGuard found — WORLDGUARD zone type enabled.");
        }
        saveResource("zones.yml", false);
        saveResource("triggers.yml", false);

        this.configManager = new ConfigManager(this);
        this.zoneManager = new ZoneManager(this);
        this.mobScalingService = new MobScalingService(this);
        this.playerJournalService = new PlayerJournalService(this);
        this.playerStatsService = new PlayerStatsService(this);

        getCommand("rpgmood").setExecutor(new RPGMoodCommand(this));
        getCommand("diary").setExecutor(new DiarioCommand(this));

        Bukkit.getPluginManager().registerEvents(new ZoneListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BlockListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MobSpawnListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MobDeathListener(this), this);
        Bukkit.getPluginManager().registerEvents(new DeathMessageListener(this), this);

        new AmbientTask(this).runTaskTimer(this, 0L, 20L);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new RPGMoodExpansion(this).register();
            getLogger().info("PlaceholderAPI found — registered %rpgmood_*% placeholders");
        }

        if (BSTATS_PLUGIN_ID > 0) {
            new Metrics(this, BSTATS_PLUGIN_ID);
        }

        getLogger().info("RPGMood enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("RPGMood disabled.");
    }

    public static RPGMoodPlugin getInstance() {
        return instance;
    }

    public ZoneManager getZoneManager() {
        return zoneManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MobScalingService getMobScalingService() {
        return mobScalingService;
    }

    public PlayerJournalService getPlayerJournalService() {
        return playerJournalService;
    }

    public PlayerStatsService getPlayerStatsService() {
        return playerStatsService;
    }

    /** True if the location is inside the named WorldGuard region. Always false if WorldGuard isn't installed. */
    public boolean isInsideWorldGuardRegion(Location location, String regionId) {
        if (!worldGuardActive) {
            return false;
        }
        try {
            return WorldGuardHook.isInsideRegion(location, regionId);
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }
}
