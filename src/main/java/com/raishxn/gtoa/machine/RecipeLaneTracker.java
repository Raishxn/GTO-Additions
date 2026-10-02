package com.raishxn.gtoa.machine;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** Tracks successful recipe lanes within a tick, independently for each input unit. */
final class RecipeLaneTracker<U, R> {
    private final Map<U, Set<R>> lanes = new IdentityHashMap<>();

    boolean contains(U unit, R recipe) {
        Set<R> recipes = lanes.get(unit);
        return recipes != null && recipes.contains(recipe);
    }

    void add(U unit, R recipe) {
        lanes.computeIfAbsent(unit, ignored -> new HashSet<>()).add(recipe);
    }

    void clear() { lanes.clear(); }
}
