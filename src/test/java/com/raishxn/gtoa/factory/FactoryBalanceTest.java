package com.raishxn.gtoa.factory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FactoryBalanceTest {
    @Test void sharedBudgetNeverExceedsTierCapacityAcrossThreadChoices() {
        var balance = new FactoryBalance();
        balance.scalingMode = "SHARED_BUDGET";
        for (int tier = 0; tier <= 20; tier++) for (int choice = 1; choice <= 300; choice++) {
            long total = (long) balance.threads(tier, choice) * balance.parallel(tier, choice, 1000, Long.MAX_VALUE);
            assertTrue(total <= balance.capacity(tier));
            assertEquals(Math.max(0, balance.capacity(tier) - total), balance.remaining(tier, total));
        }
    }
    @Test void unlimitedRequiresExplicitOptInAndStillHonorsTechnicalCap() {
        var balance = new FactoryBalance();
        balance.scalingMode = "UNLIMITED";
        assertEquals(8, balance.capacity(4));
        balance.allowUnlimited = true;
        assertEquals(1048576, balance.capacity(4));
        assertEquals(4096, balance.parallel(4, 256, 1000, 120));
        assertEquals(0, balance.remaining(4, Long.MAX_VALUE));
    }
    @Test void legacyRetainsGtnaColdWarmupOverloadAndBatchScaling() {
        var balance = new FactoryBalance();
        assertEquals(128, balance.parallel(1, 1, 1, 0));
        assertEquals(32, balance.threads(1, 1));
        assertEquals(1 + 7 * (1 - Math.exp(-1)), balance.warmup(60), 1e-12);
        assertEquals(1024, balance.parallel(1, 1, 1, 120));
        assertEquals(2048, balance.parallel(1, 1, 2, 120));
        assertEquals(Integer.MAX_VALUE, balance.parallel(20, 1, 1000, Long.MAX_VALUE));
        balance.warmupEnabled = false;
        balance.batchEnabled = false;
        assertEquals(128, balance.parallel(1, 1, 1000, Long.MAX_VALUE));
    }
    @Test void invalidConfigurationCannotBreakBudgetOrProduceNaN() {
        var balance = new FactoryBalance();
        balance.scalingMode = null;
        balance.capacityByTier = null;
        balance.maxWarmup = Double.NaN;
        balance.warmupTau = Double.POSITIVE_INFINITY;
        balance.technicalOperationCap = -1;
        balance.allowedRecipeTypes = null;
        balance.validate();
        assertTrue(balance.legacy());
        assertTrue(Double.isFinite(balance.warmup(60)));
        balance.scalingMode = "SHARED_BUDGET";
        assertEquals(1, balance.capacity(4));
        assertEquals(1, balance.threads(4, Integer.MAX_VALUE));
    }
}
