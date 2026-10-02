package com.raishxn.gtoa.factory;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class PatternOutputsTest {
    @Test void identicalInputsCannotSelectARecipeWithAnotherOutput() {
        assertFalse(PatternOutputs.matches(Map.of("foil", 4L), Map.of("plate", 1L)));
        assertTrue(PatternOutputs.matches(Map.of("foil", 4L), Map.of("foil", 4L)));
    }
    @Test void accumulatedBatchesAndOmittedByproductsRemainValid() {
        assertTrue(PatternOutputs.matches(Map.of("glass", 12L), Map.of("glass", 4L, "slag", 1L)));
        assertTrue(PatternOutputs.matches(Map.of("item", 6L, "fluid", 3000L), Map.of("item", 2L, "fluid", 1000L)));
        assertFalse(PatternOutputs.matches(Map.of("item", 6L, "fluid", 1000L), Map.of("item", 2L, "fluid", 1000L)));
    }
    @Test void largeOutputRatiosDoNotOverflowAndEmptyPatternsAreRejected() {
        assertTrue(PatternOutputs.matches(Map.of("a", Long.MAX_VALUE, "b", Long.MAX_VALUE), Map.of("a", Long.MAX_VALUE, "b", Long.MAX_VALUE)));
        assertFalse(PatternOutputs.matches(Map.of(), Map.of("a", 1L)));
    }
}
