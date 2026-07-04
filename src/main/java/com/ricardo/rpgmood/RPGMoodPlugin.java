package com.ricardo.rpgmood;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class RPGMoodPlugin extends JavaPlugin {

    private static RPGMoodPlugin instance;
    private ZoneManager zoneManager;
    private ConfigManager configManager;
    private MobScalingService mobScalingService;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveResource("zones.yml", false);
        saveResource("triggers.yml", false);

        this.configManager = new ConfigManager(this);
        this.zoneManager = new ZoneManager(this);
        this.mobScalingService = new MobScalingService(this);

        getCommand("rpgmood").setExecutor(new RPGMoodCommand(this));
        getCommand("diary").setExecutor(new DiarioCommand(this));

        Bukkit.getPluginManager().registerEvents(new ZoneListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BlockListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MobSpawnListener(this), this);
        Bukkit.getPluginManager().registerEvents(new DeathMessageListener(this), this);

        new AmbientTask(this).runTaskTimer(this, 0L, 20L);

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
}
