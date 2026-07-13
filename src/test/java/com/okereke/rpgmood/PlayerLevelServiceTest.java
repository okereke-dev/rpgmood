package com.okereke.rpgmood;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerLevelServiceTest {

    @Test
    void levelOneRequiresExactly100Xp() {
        assertEquals(100, PlayerLevelService.xpForLevel(1));
    }

    @Test
    void xpRequirementGrowsWithLevel() {
        long level5 = PlayerLevelService.xpForLevel(5);
        long level10 = PlayerLevelService.xpForLevel(10);
        assertTrue(level10 > level5);
    }

    @Test
    void zeroXpAtLevelZero() {
        assertEquals(0, PlayerLevelService.xpForLevel(0));
    }
}
