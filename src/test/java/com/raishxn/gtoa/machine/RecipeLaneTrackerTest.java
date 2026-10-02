package com.raishxn.gtoa.machine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecipeLaneTrackerTest {
    @Test
    void permitsDifferentRecipesInTheSameInputUnit() {
        var tracker = new RecipeLaneTracker<Object, String>();
        Object unit = new Object();
        tracker.add(unit, "iron");
        assertTrue(tracker.contains(unit, "iron"));
        assertFalse(tracker.contains(unit, "gold"));
    }

    @Test
    void doesNotStarveOtherInputUnitsWithTheSameRecipe() {
        var tracker = new RecipeLaneTracker<Object, String>();
        // Equal units must still be independent lanes.
        Object first = new String("unit");
        Object second = new String("unit");
        tracker.add(first, "iron");
        assertFalse(tracker.contains(second, "iron"));
    }

    @Test
    void recipesBecomeEligibleAgainOnTheNextTick() {
        var tracker = new RecipeLaneTracker<Object, String>();
        Object unit = new Object();
        tracker.add(unit, "iron");
        tracker.clear();
        assertFalse(tracker.contains(unit, "iron"));
    }

    @Test
    void selfReproducingRecipeCannotRunForeverInOneTick() {
        var tracker = new RecipeLaneTracker<Object, String>();
        Object unit = new Object();
        int executions = 0;
        while (!tracker.contains(unit, "loop")) {
            tracker.add(unit, "loop");
            executions++;
            assertTrue(executions < 2);
        }
        assertEquals(1, executions);
    }
}
