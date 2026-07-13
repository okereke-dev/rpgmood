package com.okereke.rpgmood.farming;

/** Quality tiers for Harvest Moon-style crops. Affects cooking results and sell value. */
public enum CropQuality {
    BRONZE(1, "§6"),
    SILVER(2, "§7"),
    GOLD(3, "§e");

    private final int tier;
    private final String color;

    CropQuality(int tier, String color) {
        this.tier = tier;
        this.color = color;
    }

    public int getTier() { return tier; }
    public String getColor() { return color; }

    public String getDisplayName() {
        return color + name().charAt(0) + name().substring(1).toLowerCase();
    }

    /** The bonus multiplier for this quality (Bronze=1.0, Silver=1.5, Gold=2.0). */
    public double getBonusMultiplier() {
        return 0.5 + tier * 0.5;
    }

    public static CropQuality fromTier(int tier) {
        return switch (tier) {
            case 2 -> SILVER;
            case 3 -> GOLD;
            default -> BRONZE;
        };
    }
}
