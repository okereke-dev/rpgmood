package com.okereke.rpgmood.mob;

/**
 * RPG-flavored mob modifiers rolled at spawn — pure variety/danger flavor layered on top of
 * the numeric level scaling in {@link com.okereke.rpgmood.MobScalingService}.
 */
public enum MobAffix {

    SWIFT("Swift"),
    WRAITH("Wraith"),
    BLEEDING("Bleeding"),
    POISONOUS("Poisonous"),
    REGENERATING("Regenerating"),
    CHILLING("Chilling");

    private final String displayName;

    MobAffix(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
