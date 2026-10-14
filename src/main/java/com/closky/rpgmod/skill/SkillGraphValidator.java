package com.closky.rpgmod.skill;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pure-Java validation of a skill graph (no Minecraft types, so it can be unit tested).
 * Node ids are strings; each node lists its parent ids.
 */
public final class SkillGraphValidator {
    private SkillGraphValidator() {
    }

    /** Returns a list of human readable problems; empty when the graph is valid. */
    public static List<String> validate(Map<String, ? extends Collection<String>> parentsById) {
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, ? extends Collection<String>> entry : parentsById.entrySet()) {
            for (String parent : entry.getValue()) {
                if (!parentsById.containsKey(parent)) {
                    problems.add("Node '" + entry.getKey() + "' references unknown parent '" + parent + "'");
                }
                if (parent.equals(entry.getKey())) {
                    problems.add("Node '" + entry.getKey() + "' is its own parent");
                }
            }
        }
        if (hasCycle(parentsById)) {
            problems.add("Skill graph contains a cycle");
        }
        return problems;
    }

    /** True if the parent relation contains a cycle (Kahn's algorithm). */
    public static boolean hasCycle(Map<String, ? extends Collection<String>> parentsById) {
        Map<String, Integer> indegree = new HashMap<>();
        Map<String, List<String>> children = new HashMap<>();
        for (String id : parentsById.keySet()) {
            indegree.putIfAbsent(id, 0);
        }
        for (Map.Entry<String, ? extends Collection<String>> entry : parentsById.entrySet()) {
            for (String parent : entry.getValue()) {
                if (!parentsById.containsKey(parent)) {
                    continue;
                }
                children.computeIfAbsent(parent, k -> new ArrayList<>()).add(entry.getKey());
                indegree.merge(entry.getKey(), 1, Integer::sum);
            }
        }
        Deque<String> queue = new ArrayDeque<>();
        indegree.forEach((id, deg) -> {
            if (deg == 0) {
                queue.add(id);
            }
        });
        int visited = 0;
        while (!queue.isEmpty()) {
            String id = queue.poll();
            visited++;
            for (String child : children.getOrDefault(id, List.of())) {
                if (indegree.merge(child, -1, Integer::sum) == 0) {
                    queue.add(child);
                }
            }
        }
        return visited != indegree.size();
    }

    /** Whether a node is reachable from the roots, given the set of already unlocked nodes. */
    public static boolean canUnlock(String id, Map<String, ? extends Collection<String>> parentsById, Set<String> unlocked) {
        Collection<String> parents = parentsById.get(id);
        if (parents == null || unlocked.contains(id)) {
            return false;
        }
        if (parents.isEmpty()) {
            return true;
        }
        for (String parent : parents) {
            if (unlocked.contains(parent)) {
                return true;
            }
        }
        return false;
    }

    /** Nodes whose parents are all missing from the unlocked set after removing {@code removed} (used by respec tests). */
    public static Set<String> orphansAfterRemoval(String removed, Map<String, ? extends Collection<String>> parentsById, Set<String> unlocked) {
        Set<String> remaining = new HashSet<>(unlocked);
        remaining.remove(removed);
        Set<String> orphans = new HashSet<>();
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String id : new ArrayList<>(remaining)) {
                Collection<String> parents = parentsById.get(id);
                if (parents == null || parents.isEmpty()) {
                    continue;
                }
                boolean supported = false;
                for (String parent : parents) {
                    if (remaining.contains(parent)) {
                        supported = true;
                        break;
                    }
                }
                if (!supported) {
                    remaining.remove(id);
                    orphans.add(id);
                    changed = true;
                }
            }
        }
        return orphans;
    }
}
