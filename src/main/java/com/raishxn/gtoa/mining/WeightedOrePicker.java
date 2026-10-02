package com.raishxn.gtoa.mining;

/** A completed-cycle counter makes retries choose the same ore without storing a remote chunk. */
public final class WeightedOrePicker {
    private WeightedOrePicker() {}
    public static int pick(long seed, long cycle, int[] weights) {
        long total = 0;
        for (int weight : weights) {
            if (weight <= 0) throw new IllegalArgumentException("Ore weights must be positive");
            total = Math.addExact(total, weight);
        }
        if (total == 0) throw new IllegalArgumentException("Empty ore pool");
        long value = seed + cycle * 0x9e3779b97f4a7c15L;
        value = (value ^ (value >>> 30)) * 0xbf58476d1ce4e5b9L;
        value = (value ^ (value >>> 27)) * 0x94d049bb133111ebL;
        value ^= value >>> 31;
        long roll = Math.floorMod(value, total);
        for (int i = 0; i < weights.length; i++) {
            if (roll < weights[i]) return i;
            roll -= weights[i];
        }
        throw new AssertionError("Weighted ore selection exceeded pool");
    }
}
