package com.okereke.rpgmood;

import com.okereke.rpgmood.farming.CropManager;
import com.okereke.rpgmood.farming.FarmingCommand;
import com.okereke.rpgmood.farming.RecipeManager;
import com.okereke.rpgmood.farming.SeasonManager;
import com.okereke.rpgmood.farming.SeasonTask;
import com.okereke.rpgmood.farming.animal.AnimalManager;
import com.okereke.rpgmood.farming.animal.listener.AnimalBreedListener;
import com.okereke.rpgmood.farming.animal.listener.AnimalFeedTask;
import com.okereke.rpgmood.farming.animal.listener.AnimalInteractListener;
import com.okereke.rpgmood.farming.animal.listener.AnimalProductTask;
import com.okereke.rpgmood.farming.listener.CookingListener;
import com.okereke.rpgmood.farming.listener.CropListener;
import com.okereke.rpgmood.menu.RPGMoodMenuListener;
import com.okereke.rpgmood.mob.MobAffixAuraTask;
import com.okereke.rpgmood.mob.MobAffixCombatListener;
import com.okereke.rpgmood.mob.MobAffixService;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.java.JavaPlugin;

public class RPGMoodPlugin extends JavaPlugin {

    /** bStats plugin ID — Register at https://bstats.org/what-is-my-plugin-id and update this. */
    private static final int BSTATS_PLUGIN_ID = 23883;

    private static RPGMoodPlugin instance;
    private ZoneManager zoneManager;
    private ConfigManager configManager;
    private MobScalingService mobScalingService;
    private MobAffixService mobAffixService;
    private PlayerJournalService playerJournalService;
    private PlayerStatsService playerStatsService;
    private SeasonManager seasonManager;
    private AchievementManager achievementManager;
    private MessageService messageService;
    private CropManager cropManager;
    private RecipeManager recipeManager;
    private AnimalManager animalManager;
    private DiarioCommand diarioCommand;
    private boolean worldGuardActive;

    @Override
    public void onEnable() {
        instance = this;

        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            worldGuardActive = true;
            getLogger().info("WorldGuard found — WORLDGUARD zone type enabled.");
        }
        // config.yml/zones.yml/triggers.yml are merged (new keys added, existing values kept)
        // inside ConfigManager's constructor via reload(). farming.yml has no owning manager
        // class, so it's merged here directly.
        ConfigMerge.mergeAndSave(this, "farming.yml");

        this.configManager = new ConfigManager(this);
        this.zoneManager = new ZoneManager(this);
        this.mobScalingService = new MobScalingService(this);
        this.mobAffixService = new MobAffixService(this);
        this.playerJournalService = new PlayerJournalService(this);
        this.playerStatsService = new PlayerStatsService(this);
        this.seasonManager = new SeasonManager(this);
        this.achievementManager = new AchievementManager(this);
        this.messageService = new MessageService(this);
        this.cropManager = new CropManager(this);
        this.recipeManager = new RecipeManager(this);
        this.animalManager = new AnimalManager(this);

        RPGMoodCommand rpgMoodCommand = new RPGMoodCommand(this);
        getCommand("rpgmood").setExecutor(rpgMoodCommand);
        getCommand("rpgmood").setTabCompleter(rpgMoodCommand);

        this.diarioCommand = new DiarioCommand(this);
        getCommand("diary").setExecutor(diarioCommand);
        getCommand("diary").setTabCompleter(diarioCommand);

        FarmingCommand farmingCommand = new FarmingCommand(this);
        getCommand("rpgmood-farm").setExecutor(farmingCommand);
        getCommand("rpgmood-farm").setTabCompleter(farmingCommand);

        Bukkit.getPluginManager().registerEvents(new ZoneListener(this), this);
        Bukkit.getPluginManager().registerEvents(new BlockListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MobSpawnListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MobDeathListener(this), this);
        Bukkit.getPluginManager().registerEvents(new DeathMessageListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CropListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CookingListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AnimalInteractListener(this), this);
        Bukkit.getPluginManager().registerEvents(new AnimalBreedListener(this), this);
        Bukkit.getPluginManager().registerEvents(new LightningListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new RPGMoodMenuListener(), this);
        Bukkit.getPluginManager().registerEvents(new RPGLootAchievementListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MobAffixCombatListener(this), this);

        new AmbientTask(this).runTaskTimer(this, 0L, 20L);
        new MobAuraEffect(this).runTaskTimer(this, 0L, 20L);
        new MobAffixAuraTask(this).runTaskTimer(this, 0L, 20L);
        new SeasonTask(this).runTaskTimer(this, 0L, 1L);
        new AnimalProductTask(this).runTaskTimer(this, 0L, 20L);
        new AnimalFeedTask(this).runTaskTimer(this, 0L, 1200L);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new RPGMoodExpansion(this).register();
            getLogger().info("PlaceholderAPI found — registered %rpgmood_*% placeholders");
        }

        if (BSTATS_PLUGIN_ID > 0) {
            try {
                new Metrics(this, BSTATS_PLUGIN_ID);
                getLogger().info("bStats metrics enabled (ID: " + BSTATS_PLUGIN_ID + ")");
            } catch (Exception e) {
                getLogger().warning("bStats failed to initialise: " + e.getMessage());
            }
        }

        getLogger().info("RPGMood v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("RPGMood disabled.");
    }

    public static RPGMoodPlugin getInstance() {
        return instance;
    }

    public ZoneManager getZoneManager() { return zoneManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public MobScalingService getMobScalingService() { return mobScalingService; }
    public MobAffixService getMobAffixService() { return mobAffixService; }
    public PlayerJournalService getPlayerJournalService() { return playerJournalService; }
    public PlayerStatsService getPlayerStatsService() { return playerStatsService; }
    public SeasonManager getSeasonManager() { return seasonManager; }
    public AchievementManager getAchievementManager() { return achievementManager; }
    public MessageService getMessageService() { return messageService; }
    public CropManager getCropManager() { return cropManager; }
    public RecipeManager getRecipeManager() { return recipeManager; }
    public AnimalManager getAnimalManager() { return animalManager; }
    public DiarioCommand getDiarioCommand() { return diarioCommand; }

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
