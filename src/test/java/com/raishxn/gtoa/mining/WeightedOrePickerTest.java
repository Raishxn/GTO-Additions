package com.raishxn.gtoa.mining;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeightedOrePickerTest {
    @Test void retriesAndRestartsKeepTheSamePendingSelection() {
        int[] weights = {3, 7, 11};
        for (long cycle = 0; cycle < 1000; cycle++) {
            int chosen = WeightedOrePicker.pick(1234567L, cycle, weights);
            assertEquals(chosen, WeightedOrePicker.pick(1234567L, cycle, weights.clone()));
            assertTrue(chosen >= 0 && chosen < weights.length);
        }
    }
    @Test void selectionFollowsTheDepositsRelativeWeights() {
        int[] counts = new int[3];
        for (long cycle = 0; cycle < 100000; cycle++)
            counts[WeightedOrePicker.pick(71234L, cycle, new int[] {1, 3, 6})]++;
        assertEquals(10000, counts[0], 1000);
        assertEquals(30000, counts[1], 1000);
        assertEquals(60000, counts[2], 1000);
    }
    @Test void largeWeightsDoNotOverflowAnIntegerTotal() {
        int[] weights = {Integer.MAX_VALUE, Integer.MAX_VALUE};
        boolean first = false, second = false;
        for (long cycle = 0; cycle < 100; cycle++) {
            int chosen = WeightedOrePicker.pick(Long.MAX_VALUE, cycle, weights);
            first |= chosen == 0;
            second |= chosen == 1;
        }
        assertTrue(first && second);
    }
    @Test void invalidPoolsAreRejected() {
        for (int[] weights : new int[][] { {}, {0}, {-1}, {2, 0} })
            assertThrows(IllegalArgumentException.class, () -> WeightedOrePicker.pick(1, 1, weights));
    }
    @Test void cycleCounterWraparoundStillSelectsOnlyValidEntries() {
        for (long cycle : new long[] {Long.MIN_VALUE, Long.MAX_VALUE, -1, 0})
            assertEquals(0, WeightedOrePicker.pick(Long.MIN_VALUE, cycle, new int[] {12}));
    }
}
