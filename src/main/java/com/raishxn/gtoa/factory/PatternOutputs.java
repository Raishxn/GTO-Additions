package com.raishxn.gtoa.factory;

import java.math.BigInteger;
import java.util.Map;

/** Pattern outputs may omit byproducts and encode several recipe batches. */
public final class PatternOutputs {
    private PatternOutputs() {}
    public static <K> boolean matches(Map<K, Long> expected, Map<K, Long> actual) {
        if (expected.isEmpty()) return false;
        BigInteger firstExpected = null, firstActual = null;
        for (var entry : expected.entrySet()) {
            long amount = actual.getOrDefault(entry.getKey(), 0L);
            if (entry.getValue() <= 0 || amount <= 0) return false;
            var want = BigInteger.valueOf(entry.getValue());
            var have = BigInteger.valueOf(amount);
            if (firstExpected == null) { firstExpected = want; firstActual = have; }
            else if (!want.multiply(firstActual).equals(have.multiply(firstExpected))) return false;
        }
        return true;
    }
}
