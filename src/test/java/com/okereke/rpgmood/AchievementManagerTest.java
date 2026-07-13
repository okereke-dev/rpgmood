package com.okereke.rpgmood;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AchievementManagerTest {

    private static final int[] SLAYER_THRESHOLDS = {10, 100, 500};
    private static final String[] SLAYER_IDS = {"slayer_initiate", "slayer_veteran", "slayer_legend"};

    @Test
    void belowFirstThresholdUnlocksNothing() {
        List<String> result = AchievementManager.achievementsForCount(9, SLAYER_THRESHOLDS, SLAYER_IDS);
        assertTrue(result.isEmpty());
    }

    @Test
    void exactlyAtThresholdUnlocksThatTier() {
        List<String> result = AchievementManager.achievementsForCount(10, SLAYER_THRESHOLDS, SLAYER_IDS);
        assertEquals(List.of("slayer_initiate"), result);
    }

    @Test
    void betweenThresholdsOnlyUnlocksLowerTiers() {
        List<String> result = AchievementManager.achievementsForCount(150, SLAYER_THRESHOLDS, SLAYER_IDS);
        assertEquals(List.of("slayer_initiate", "slayer_veteran"), result);
    }

    @Test
    void pastHighestThresholdUnlocksAllTiers() {
        List<String> result = AchievementManager.achievementsForCount(500, SLAYER_THRESHOLDS, SLAYER_IDS);
        assertEquals(List.of("slayer_initiate", "slayer_veteran", "slayer_legend"), result);
    }

    @Test
    void singleThresholdMatchesFarmerAchievement() {
        List<String> result = AchievementManager.achievementsForCount(50, new int[]{50}, new String[]{"farmer"});
        assertEquals(List.of("farmer"), result);
    }

    @Test
    void catalogHasNoDuplicateIds() {
        Set<String> seen = new HashSet<>();
        for (AchievementManager.Achievement ach : AchievementManager.ALL_ACHIEVEMENTS) {
            assertTrue(seen.add(ach.id()), "Duplicate achievement id: " + ach.id());
        }
    }

    @Test
    void catalogHasNoBlankFields() {
        for (AchievementManager.Achievement ach : AchievementManager.ALL_ACHIEVEMENTS) {
            assertFalse(ach.id().isBlank(), "Blank id");
            assertFalse(ach.name().isBlank(), "Blank name for " + ach.id());
            assertFalse(ach.description().isBlank(), "Blank description for " + ach.id());
            assertFalse(ach.icon().isBlank(), "Blank icon for " + ach.id());
        }
    }

    @Test
    void everyCategoryHasAtLeastOneAchievement() {
        for (AchievementManager.Category category : AchievementManager.Category.values()) {
            boolean present = AchievementManager.ALL_ACHIEVEMENTS.stream().anyMatch(a -> a.category() == category);
            assertTrue(present, "No achievements in category " + category);
        }
    }
}
