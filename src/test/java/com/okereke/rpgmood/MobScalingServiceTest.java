package com.okereke.rpgmood;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MobScalingServiceTest {

    @Test
    void atSpawnWithNoBonusesLevelIsOne() {
        int level = MobScalingService.calculateDifficultyLevel(1, 0, 0, 0, 0, 40, 180.0, 1);
        assertEquals(1, level);
    }

    @Test
    void combinesBaseDistanceBiomeStructureAndPlayerBonuses() {
        // baseLevel 5 -> adjustedBase 3; distance 900 -> radialLevel 5 -> +4; biome +2; structure +3; 2 players * 1 -> +2
        int level = MobScalingService.calculateDifficultyLevel(5, 900.0, 2, 3, 2, 40, 180.0, 1);
        assertEquals(14, level);
    }

    @Test
    void neverExceedsMaxLevel() {
        int level = MobScalingService.calculateDifficultyLevel(5, 100000.0, 10, 10, 10, 40, 180.0, 1);
        assertEquals(40, level);
    }

    @Test
    void neverGoesBelowOneEvenWithNegativeInputs() {
        int level = MobScalingService.calculateDifficultyLevel(0, 0, 0, 0, 0, 40, 180.0, 1);
        assertEquals(1, level);
    }

    @Test
    void radialLevelOnlyContributesBeyondFirstStep() {
        // baseLevel 3 -> adjustedBase 1, isolating the radial contribution from the base/floor clamp.
        // distance 180 -> radialLevel 1 -> max(0, 1-1) = 0 contribution -> total 1
        int atOneStep = MobScalingService.calculateDifficultyLevel(3, 180.0, 0, 0, 0, 40, 180.0, 1);
        // distance 360 -> radialLevel 2 -> max(0, 2-1) = 1 contribution -> total 2
        int atTwoSteps = MobScalingService.calculateDifficultyLevel(3, 360.0, 0, 0, 0, 40, 180.0, 1);
        assertEquals(1, atOneStep);
        assertEquals(2, atTwoSteps);
    }
}
