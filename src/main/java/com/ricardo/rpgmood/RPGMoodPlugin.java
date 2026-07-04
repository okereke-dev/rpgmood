package com.ricardo.rpgmood;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public class RPGMoodPlugin extends JavaPlugin {

    private static RPGMoodPlugin instance;
    private ZoneManager zoneManager;
    private ConfigManager configManager;
    private MobScalingService mobScalingService;
    private PlayerJournalService playerJournalService;
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

        getCommand("rpgmood").setExecutor(new RPGMoodCommand(this));
        getCommand("diary").setExecutor(new DiarioCommand(this));
        getCommand("emote").setExecutor(new EmoteCommand(this));

        Bukkit.getPluginManager().registerEvents(new ZoneListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BlockListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MobSpawnListener(this), this);
        Bukkit.getPluginManager().registerEvents(new DeathMessageListener(this), this);

        new AmbientTask(this).runTaskTimer(this, 0L, 20L);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new RPGMoodExpansion(this).register();
            getLogger().info("PlaceholderAPI found — registered %rpgmood_*% placeholders");
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
