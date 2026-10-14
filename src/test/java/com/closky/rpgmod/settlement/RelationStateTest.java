package com.closky.rpgmod.settlement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RelationStateTest {

    @Test
    void boundaries() {
        assertEquals(RelationState.WAR, RelationState.of(-100));
        assertEquals(RelationState.WAR, RelationState.of(-71));
        assertEquals(RelationState.HOSTILE, RelationState.of(-70));
        assertEquals(RelationState.HOSTILE, RelationState.of(-41));
        assertEquals(RelationState.TENSE, RelationState.of(-40));
        assertEquals(RelationState.TENSE, RelationState.of(-11));
        assertEquals(RelationState.NEUTRAL, RelationState.of(-10));
        assertEquals(RelationState.NEUTRAL, RelationState.of(0));
        assertEquals(RelationState.NEUTRAL, RelationState.of(39));
        assertEquals(RelationState.FRIENDLY, RelationState.of(40));
        assertEquals(RelationState.FRIENDLY, RelationState.of(79));
        assertEquals(RelationState.ALLIED, RelationState.of(80));
        assertEquals(RelationState.ALLIED, RelationState.of(100));
    }

    @Test
    void outOfRangeValuesAreClamped() {
        assertEquals(RelationState.WAR, RelationState.of(-10_000));
        assertEquals(RelationState.ALLIED, RelationState.of(10_000));
    }

    @Test
    void rangesAreContiguousAndCoverEverything() {
        RelationState[] states = RelationState.values();
        assertEquals(RelationMath.MIN, states[0].getMin());
        assertEquals(RelationMath.MAX, states[states.length - 1].getMax());
        for (int i = 1; i < states.length; i++) {
            assertEquals(states[i - 1].getMax() + 1, states[i].getMin(), "gap between " + states[i - 1] + " and " + states[i]);
        }
        for (int value = RelationMath.MIN; value <= RelationMath.MAX; value++) {
            RelationState state = RelationState.of(value);
            assertNotNull(state);
            assertEquals(true, value >= state.getMin() && value <= state.getMax(), "value " + value + " mapped to " + state);
        }
    }

    @Test
    void translationKeys() {
        assertEquals("relation.rpgmod.war", RelationState.WAR.getTranslationKey());
        assertEquals("allied", RelationState.ALLIED.getKey());
    }

    @Test
    void warThresholdMatchesState() {
        assertEquals(RelationState.WAR, RelationState.of(RelationMath.WAR_THRESHOLD));
        assertEquals(RelationState.TENSE, RelationState.of(RelationMath.PEACE_THRESHOLD));
    }
}
