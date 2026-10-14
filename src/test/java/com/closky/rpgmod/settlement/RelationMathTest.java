package com.closky.rpgmod.settlement;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RelationMathTest {

    @Test
    void clampKeepsValuesInsideRange() {
        assertEquals(RelationMath.MIN, RelationMath.clamp(-500));
        assertEquals(RelationMath.MAX, RelationMath.clamp(500));
        assertEquals(17, RelationMath.clamp(17));
    }

    @Test
    void decayMovesTowardsZeroWithoutCrossingIt() {
        assertEquals(5, RelationMath.decay(8, 3));
        assertEquals(-5, RelationMath.decay(-8, 3));
        assertEquals(0, RelationMath.decay(2, 5));
        assertEquals(0, RelationMath.decay(-2, 5));
        assertEquals(0, RelationMath.decay(0, 5));
        assertEquals(40, RelationMath.decay(40, 0));
    }

    @Test
    void warRequiresHatredStrengthAndLuck() {
        assertTrue(RelationMath.shouldDeclareWar(RelationMath.WAR_THRESHOLD, 30, 0.5D, 0.1D));
        assertFalse(RelationMath.shouldDeclareWar(RelationMath.WAR_THRESHOLD + 1, 30, 0.5D, 0.1D), "relation above threshold");
        assertFalse(RelationMath.shouldDeclareWar(-100, 29, 0.5D, 0.1D), "weak aggressor");
        assertFalse(RelationMath.shouldDeclareWar(-100, 80, 0.5D, 0.9D), "bad roll");
        assertFalse(RelationMath.shouldDeclareWar(-100, 80, 0.0D, 0.0D), "wars disabled by frequency 0");
    }

    @Test
    void peaceConditions() {
        assertTrue(RelationMath.shouldMakePeace(RelationMath.PEACE_THRESHOLD + 1, 50, 50, 0), "relations recovered");
        assertTrue(RelationMath.shouldMakePeace(-90, 5, 50, 0), "attacker exhausted");
        assertTrue(RelationMath.shouldMakePeace(-90, 50, 4, 0), "defender exhausted");
        assertTrue(RelationMath.shouldMakePeace(-90, 50, 50, 12), "war fatigue");
        assertFalse(RelationMath.shouldMakePeace(-90, 50, 50, 11));
    }

    @Test
    void battleLossesAreAlwaysPositiveAndBounded() {
        Random random = new Random(1234L);
        for (int i = 0; i < 1000; i++) {
            int[] losses = RelationMath.resolveBattle(random.nextInt(100), random.nextInt(100), random);
            assertEquals(2, losses.length);
            for (int loss : losses) {
                assertTrue(loss >= 2 && loss <= 12, "loss out of range: " + loss);
            }
        }
    }

    @Test
    void strongerAttackerWinsMoreOften() {
        Random random = new Random(42L);
        int attackerWins = 0;
        for (int i = 0; i < 2000; i++) {
            int[] losses = RelationMath.resolveBattle(90, 10, random);
            if (losses[0] < losses[1]) {
                attackerWins++;
            }
        }
        assertTrue(attackerWins > 1500, "attacker won only " + attackerWins + " / 2000");
    }

    @Test
    void battleIsDeterministicForSeed() {
        assertArrayEquals(RelationMath.resolveBattle(40, 60, new Random(7L)), RelationMath.resolveBattle(40, 60, new Random(7L)));
    }

    @Test
    void priceMultiplierRange() {
        assertEquals(1.0F, RelationMath.priceMultiplier(0), 1.0E-6F);
        assertEquals(0.8F, RelationMath.priceMultiplier(100), 1.0E-6F);
        assertEquals(1.3F, RelationMath.priceMultiplier(-100), 1.0E-6F);
        assertEquals(0.8F, RelationMath.priceMultiplier(1000), 1.0E-6F);
        assertTrue(RelationMath.priceMultiplier(50) < RelationMath.priceMultiplier(-50));
    }

    @Test
    void allyOutrageIsNegative() {
        assertTrue(RelationMath.allyOutrage() < 0);
    }
}
