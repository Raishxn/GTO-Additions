package com.raishxn.gtoa.machine;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EntangledRatesTest {
    @Test void steamThroughEvDoublesFromFourToSixtyFourPerSecond() {
        int[] expected = {4, 8, 16, 32, 64};
        assertEquals(20, EntangledRates.CYCLE_TICKS);
        for (int level = 0; level < expected.length; level++)
            assertEquals(expected[level], EntangledRates.rawPerSecond(level));
        for (int level = 0; level < expected.length; level++)
            assertEquals(expected[level], EntangledRates.rawPerCycle(level) * 20 / EntangledRates.MINER_CYCLE_TICKS);
    }
    @Test void oilDrillUsesOnlyTheLinkedFieldsYieldAndDoublesEachTier() {
        int[] expected = {200, 400, 800, 1600, 3200};
        for (int level = 0; level < expected.length; level++)
            assertEquals(expected[level], EntangledRates.fluidPerCycle(200, level));
        assertEquals(80, EntangledRates.COAL_BURN_TICKS / 20);
    }
    @Test void invalidOilYieldAndNumericOverflowAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> EntangledRates.fluidPerCycle(0, 1));
        assertThrows(IllegalArgumentException.class, () -> EntangledRates.fluidPerCycle(-1, 1));
        assertThrows(ArithmeticException.class, () -> EntangledRates.fluidPerCycle(Integer.MAX_VALUE, 4));
    }
    @Test void anEvBatchWithItsMaximumTierCoverFitsNineOrdinaryOutputSlots() {
        int coveredBatch = EntangledRates.rawPerCycle(4) * 32;
        assertTrue(coveredBatch <= 9 * 64);
        assertEquals(10240, coveredBatch * 20); // cover divides a five-tick cycle by five
    }
    @Test void tiersOutsideTheRequestedRangeAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> EntangledRates.rawPerCycle(-1));
        assertThrows(IllegalArgumentException.class, () -> EntangledRates.rawPerCycle(5));
    }
}
