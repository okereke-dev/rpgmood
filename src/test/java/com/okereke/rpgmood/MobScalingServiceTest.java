package com.okereke.rpgmood;

import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void nightBonusAddsOnTopOfDaytimeLevel() {
        // Same non-degenerate inputs as combinesBaseDistanceBiomeStructureAndPlayerBonuses
        // (day total 14), so the +2 night bonus doesn't get masked by the level-1 floor clamp.
        int day = MobScalingService.calculateDifficultyLevel(5, 900.0, 2, 3, 2, 40, 180.0, 1, 0);
        int night = MobScalingService.calculateDifficultyLevel(5, 900.0, 2, 3, 2, 40, 180.0, 1, 2);
        assertEquals(day + 2, night);
    }

    @Test
    void nightBonusStillClampsToMaxLevel() {
        int level = MobScalingService.calculateDifficultyLevel(5, 100000.0, 10, 10, 10, 40, 180.0, 1, 2);
        assertEquals(40, level);
    }

    @Test
    void statMultiplierAtLevelOneEqualsEarlyGameFraction() {
        double multiplier = MobScalingService.calculateStatMultiplier(1, 0.85, 8);
        assertEquals(0.85, multiplier, 0.0001);
    }

    @Test
    void statMultiplierReachesExactParityAtParityLevel() {
        double multiplier = MobScalingService.calculateStatMultiplier(8, 0.85, 8);
        assertEquals(1.0, multiplier, 0.0001);
    }

    @Test
    void statMultiplierKeepsClimbingLinearlyPastParity() {
        double atParity = MobScalingService.calculateStatMultiplier(8, 0.85, 8);
        double doubleParityLevel = MobScalingService.calculateStatMultiplier(15, 0.85, 8);
        assertTrue(doubleParityLevel > atParity);
    }

    @Test
    void bossTypesIncludeWardenWitherElderGuardianAndDragon() {
        assertTrue(MobScalingService.isBoss(EntityType.WARDEN));
        assertTrue(MobScalingService.isBoss(EntityType.WITHER));
        assertTrue(MobScalingService.isBoss(EntityType.ELDER_GUARDIAN));
        assertTrue(MobScalingService.isBoss(EntityType.ENDER_DRAGON));
        assertFalse(MobScalingService.isBoss(EntityType.ZOMBIE));
    }

    @Test
    void bossFloorKeepsMultiplierAtLeastOneBelowParity() {
        double raw = MobScalingService.calculateStatMultiplier(1, 0.85, 8);
        assertEquals(0.85, raw, 0.0001);
        assertEquals(1.0, MobScalingService.applyBossFloor(EntityType.WARDEN, raw), 0.0001);
        assertEquals(0.85, MobScalingService.applyBossFloor(EntityType.ZOMBIE, raw), 0.0001);
    }

    @Test
    void bossFloorDoesNotCapAboveParity() {
        double raw = MobScalingService.calculateStatMultiplier(20, 0.85, 8);
        assertTrue(raw > 1.0);
        assertEquals(raw, MobScalingService.applyBossFloor(EntityType.WARDEN, raw), 0.0001);
    }

    @Test
    void knockbackResistStartsAtMinLevel() {
        assertEquals(0.0, MobScalingService.calculateKnockbackResist(24, 25, 0.02, 0.85), 0.0001);
        assertEquals(0.0, MobScalingService.calculateKnockbackResist(25, 25, 0.02, 0.85), 0.0001);
        assertEquals(0.10, MobScalingService.calculateKnockbackResist(30, 25, 0.02, 0.85), 0.0001);
    }

    @Test
    void knockbackResistClampsToMax() {
        assertEquals(0.85, MobScalingService.calculateKnockbackResist(100, 25, 0.02, 0.85), 0.0001);
    }

    @Test
    void attackSpeedBonusStartsAtMinLevel() {
        assertEquals(0.0, MobScalingService.calculateAttackSpeedBonus(19, 20, 0.02, 0.4), 0.0001);
        assertEquals(0.0, MobScalingService.calculateAttackSpeedBonus(20, 20, 0.02, 0.4), 0.0001);
        assertEquals(0.20, MobScalingService.calculateAttackSpeedBonus(30, 20, 0.02, 0.4), 0.0001);
    }

    @Test
    void attackSpeedBonusClampsToMax() {
        assertEquals(0.4, MobScalingService.calculateAttackSpeedBonus(100, 20, 0.02, 0.4), 0.0001);
    }

    @Test
    void followRangeCapClampsHighValues() {
        assertEquals(36.0, MobScalingService.clampFollowRange(80.0, 36.0), 0.0001);
        assertEquals(20.0, MobScalingService.clampFollowRange(20.0, 36.0), 0.0001);
        assertEquals(80.0, MobScalingService.clampFollowRange(80.0, 0.0), 0.0001);
    }
}
