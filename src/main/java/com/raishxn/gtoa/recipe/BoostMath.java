package com.raishxn.gtoa.recipe;

/** Arithmetic bounds used by output simulation and singleblock recipe modifiers. */
public final class BoostMath {
    private BoostMath() {}
    public static int limit(int tier) {
        if (tier < 0 || tier > 14) throw new IllegalArgumentException("Tier outside ULV–MAX: " + tier);
        return 1 << (tier + 1);
    }
    public static long amount(long amount, int multiplier) {
        if (amount < 0 || multiplier < 1) throw new IllegalArgumentException("Invalid output amount or multiplier");
        return Math.multiplyExact(amount, multiplier);
    }
    public static int duration(int ticks) { return Math.max(1, (int) (((long) ticks + 4) / 5)); }
    public static long energy(long eut) { return eut > 0 ? eut / 2 + eut % 2 : eut; }
}
