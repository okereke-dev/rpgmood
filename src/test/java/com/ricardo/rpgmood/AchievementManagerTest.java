package com.ricardo.rpgmood;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
