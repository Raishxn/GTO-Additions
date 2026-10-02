package com.raishxn.gtoa.machine;

/** Level 0 is coal-fired Steam; levels 1..4 are LV..EV. */
public final class EntangledRates {
    public static final int CYCLE_TICKS = 20;
    public static final int MINER_CYCLE_TICKS = 5;
    public static final int COAL_BURN_TICKS = 1600;
    private EntangledRates() {}
    public static int multiplier(int level) {
        if (level < 0 || level > 4) throw new IllegalArgumentException("Entangled tiers are Steam through EV");
        return 1 << level;
    }
    public static int rawPerSecond(int level) { return 4 * multiplier(level); }
    public static int rawPerCycle(int level) { return multiplier(level); }
    public static int fluidPerCycle(int yield, int level) {
        if (yield <= 0) throw new IllegalArgumentException("Field yield must be positive");
        return Math.multiplyExact(yield, multiplier(level));
    }
}
