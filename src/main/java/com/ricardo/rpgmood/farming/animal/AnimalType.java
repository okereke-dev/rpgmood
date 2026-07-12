package com.ricardo.rpgmood.farming.animal;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

/**
 * Supported animal species with their base product and favorite food.
 */
public enum AnimalType {

    COW("Cow", EntityType.COW, Material.MILK_BUCKET, Material.WHEAT, "🌸🌞"),
    CHICKEN("Chicken", EntityType.CHICKEN, Material.EGG, Material.WHEAT_SEEDS, "🌸🌞🍂❄️"),
    SHEEP("Sheep", EntityType.SHEEP, Material.WHITE_WOOL, Material.WHEAT, "🌸🌞"),
    GOAT("Goat", EntityType.GOAT, Material.MILK_BUCKET, Material.WHEAT, "🌞🍂");

    private final String displayName;
    private final EntityType entityType;
    private final Material baseProduct;
    private final Material favoriteFood;
    private final String idealSeasons;

    AnimalType(String displayName, EntityType entityType, Material baseProduct,
               Material favoriteFood, String idealSeasons) {
        this.displayName = displayName;
        this.entityType = entityType;
        this.baseProduct = baseProduct;
        this.favoriteFood = favoriteFood;
        this.idealSeasons = idealSeasons;
    }

    public String getDisplayName() { return displayName; }
    public EntityType getEntityType() { return entityType; }
    public Material getBaseProduct() { return baseProduct; }
    public Material getFavoriteFood() { return favoriteFood; }
    public String getIdealSeasons() { return idealSeasons; }

    /** Returns the cost configuration key for purchasing this animal. */
    public String getCostKey() { return name().toLowerCase(); }
}
