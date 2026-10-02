package com.raishxn.gtoa.recipe;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class BoostMathTest {
    @Test void everyTierDoublesThroughMax() {
        assertEquals(2, BoostMath.limit(0));
        for (int tier = 1; tier <= 14; tier++) assertEquals(BoostMath.limit(tier - 1) * 2, BoostMath.limit(tier));
        assertEquals(32768, BoostMath.limit(14));
        assertThrows(IllegalArgumentException.class, () -> BoostMath.limit(15));
    }
    @Test void largeAmountsRemainLongAndOverflowRefusesOutputRatherThanCreatingWrappedCounts() {
        assertEquals(32_768_000_000_000L, BoostMath.amount(1_000_000_000L, BoostMath.limit(14)));
        assertThrows(ArithmeticException.class, () -> BoostMath.amount(Long.MAX_VALUE, 2));
        assertThrows(IllegalArgumentException.class, () -> BoostMath.amount(-1, 2));
    }
    @Test void acceleratedRecipesAlwaysTakeAtLeastOneTickAndRoundUp() {
        assertEquals(1, BoostMath.duration(1));
        assertEquals(1, BoostMath.duration(5));
        assertEquals(2, BoostMath.duration(6));
        assertEquals(20, BoostMath.duration(100));
        assertEquals(429496730, BoostMath.duration(Integer.MAX_VALUE));
    }
    @Test void energyNeverWrapsOrBoostsGeneratorsAndOddConsumptionRoundsUp() {
        assertEquals(16, BoostMath.energy(32));
        assertEquals(2, BoostMath.energy(3));
        assertEquals(1, BoostMath.energy(1));
        assertEquals(0, BoostMath.energy(0));
        assertEquals(-32, BoostMath.energy(-32));
        assertEquals(4611686018427387904L, BoostMath.energy(Long.MAX_VALUE));
    }
}
