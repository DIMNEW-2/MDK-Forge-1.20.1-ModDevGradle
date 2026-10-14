package com.closky.rpgmod.skill;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillGraphValidatorTest {
    private static final Map<String, List<String>> VALID = Map.of(
            "combat/strength_1", List.of(),
            "combat/strength_2", List.of("combat/strength_1"),
            "combat/swift_strikes", List.of("combat/strength_1"),
            "combat/executioner", List.of("combat/strength_2"),
            "arcane/archmage", List.of("combat/strength_2", "combat/swift_strikes"));

    @Test
    void validGraphHasNoProblems() {
        assertTrue(SkillGraphValidator.validate(VALID).isEmpty());
        assertFalse(SkillGraphValidator.hasCycle(VALID));
    }

    @Test
    void unknownParentIsReported() {
        Map<String, List<String>> graph = Map.of("a", List.of(), "b", List.of("missing"));
        List<String> problems = SkillGraphValidator.validate(graph);
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("missing"));
    }

    @Test
    void selfParentIsReportedAsCycle() {
        Map<String, List<String>> graph = Map.of("a", List.of("a"));
        List<String> problems = SkillGraphValidator.validate(graph);
        assertTrue(problems.stream().anyMatch(p -> p.contains("own parent")));
        assertTrue(SkillGraphValidator.hasCycle(graph));
    }

    @Test
    void cyclesAreDetected() {
        Map<String, List<String>> graph = Map.of("a", List.of("c"), "b", List.of("a"), "c", List.of("b"), "root", List.of());
        assertTrue(SkillGraphValidator.hasCycle(graph));
        assertTrue(SkillGraphValidator.validate(graph).stream().anyMatch(p -> p.contains("cycle")));
    }

    @Test
    void canUnlockFollowsParents() {
        assertTrue(SkillGraphValidator.canUnlock("combat/strength_1", VALID, Set.of()), "roots are always available");
        assertFalse(SkillGraphValidator.canUnlock("combat/strength_2", VALID, Set.of()), "parent not unlocked");
        assertTrue(SkillGraphValidator.canUnlock("combat/strength_2", VALID, Set.of("combat/strength_1")));
        assertFalse(SkillGraphValidator.canUnlock("combat/strength_1", VALID, Set.of("combat/strength_1")), "already unlocked");
        assertFalse(SkillGraphValidator.canUnlock("unknown", VALID, Set.of()));
        // any single parent is enough (OR semantics)
        assertTrue(SkillGraphValidator.canUnlock("arcane/archmage", VALID, Set.of("combat/strength_1", "combat/swift_strikes")));
    }

    @Test
    void removingANodeOrphansItsDescendants() {
        Set<String> unlocked = Set.of("combat/strength_1", "combat/strength_2", "combat/executioner", "combat/swift_strikes");
        Set<String> orphans = SkillGraphValidator.orphansAfterRemoval("combat/strength_1", VALID, unlocked);
        assertEquals(Set.of("combat/strength_2", "combat/executioner", "combat/swift_strikes"), orphans);
        assertEquals(Set.of("combat/executioner"), SkillGraphValidator.orphansAfterRemoval("combat/strength_2", VALID, unlocked));
        assertTrue(SkillGraphValidator.orphansAfterRemoval("combat/executioner", VALID, unlocked).isEmpty());
    }
}
