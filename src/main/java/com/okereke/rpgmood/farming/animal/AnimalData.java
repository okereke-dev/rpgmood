package com.okereke.rpgmood.farming.animal;

import org.bukkit.configuration.ConfigurationSection;

import java.util.UUID;

/**
 * Persistent data for a single owned animal.
 * Stored in animals.yml, keyed by the animal's entity UUID.
 */
public class AnimalData {

    private final UUID animalId;
    private final AnimalType type;
    private final UUID ownerId;
    private String name;
    private double affection;      // 0.0 - 10.0
    private double health;         // 0.0 - 1.0
    private double hunger;         // 0.0 - 1.0
    private int lastFedDay;        // server day last fed
    private int lastProductDay;    // server day last produced
    private int lastInteractionDay; // server day last petted
    private boolean sick;
    private boolean inside;

    public AnimalData(UUID animalId, AnimalType type, UUID ownerId, String name) {
        this.animalId = animalId;
        this.type = type;
        this.ownerId = ownerId;
        this.name = name;
        this.affection = 0.0;
        this.health = 1.0;
        this.hunger = 1.0;
        this.lastFedDay = -1;
        this.lastProductDay = -1;
        this.lastInteractionDay = -1;
        this.sick = false;
        this.inside = true;
    }

    /** Loads from a YAML configuration section. */
    public static AnimalData load(UUID animalId, ConfigurationSection section) {
        AnimalType type = AnimalType.valueOf(section.getString("type", "COW"));
        UUID owner = UUID.fromString(section.getString("owner", ""));
        String name = section.getString("name", type.getDisplayName());
        AnimalData data = new AnimalData(animalId, type, owner, name);
        data.affection = section.getDouble("affection", 0.0);
        data.health = section.getDouble("health", 1.0);
        data.hunger = section.getDouble("hunger", 1.0);
        data.lastFedDay = section.getInt("lastFedDay", -1);
        data.lastProductDay = section.getInt("lastProductDay", -1);
        data.lastInteractionDay = section.getInt("lastInteractionDay", -1);
        data.sick = section.getBoolean("sick", false);
        data.inside = section.getBoolean("inside", true);
        return data;
    }

    /** Saves to a YAML configuration section. */
    public void save(ConfigurationSection section) {
        section.set("type", type.name());
        section.set("owner", ownerId.toString());
        section.set("name", name);
        section.set("affection", affection);
        section.set("health", health);
        section.set("hunger", hunger);
        section.set("lastFedDay", lastFedDay);
        section.set("lastProductDay", lastProductDay);
        section.set("lastInteractionDay", lastInteractionDay);
        section.set("sick", sick);
        section.set("inside", inside);
    }

    // -- Getters --

    public UUID getAnimalId() { return animalId; }
    public AnimalType getType() { return type; }
    public UUID getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public double getAffection() { return affection; }
    public double getHealth() { return health; }
    public double getHunger() { return hunger; }
    public int getLastFedDay() { return lastFedDay; }
    public int getLastProductDay() { return lastProductDay; }
    public int getLastInteractionDay() { return lastInteractionDay; }
    public boolean isSick() { return sick; }
    public boolean isInside() { return inside; }

    // -- Setters --

    public void setName(String name) { this.name = name; }
    public void setAffection(double affection) { this.affection = Math.max(0.0, Math.min(10.0, affection)); }
    public void setHealth(double health) { this.health = Math.max(0.0, Math.min(1.0, health)); }
    public void setHunger(double hunger) { this.hunger = Math.max(0.0, Math.min(1.0, hunger)); }
    public void setLastFedDay(int day) { this.lastFedDay = day; }
    public void setLastProductDay(int day) { this.lastProductDay = day; }
    public void setLastInteractionDay(int day) { this.lastInteractionDay = day; }
    public void setSick(boolean sick) { this.sick = sick; }
    public void setInside(boolean inside) { this.inside = inside; }

    /** Current affection heart level (0-5). */
    public int getHeartLevel() {
        if (affection >= 9.1) return 5;
        if (affection >= 7.1) return 4;
        if (affection >= 5.1) return 3;
        if (affection >= 3.1) return 2;
        if (affection >= 1.1) return 1;
        return 0;
    }

    /** Whether the animal was fed today (checks against current server day). */
    public boolean isFedToday(int currentDay) {
        return lastFedDay == currentDay;
    }

    /** Whether the animal can produce today (hasn't produced today). */
    public boolean canProduceToday(int currentDay) {
        return lastProductDay != currentDay;
    }
}
