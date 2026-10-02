package com.raishxn.gtoa.factory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** GTNA scaling rules, independent of the game so budget invariants can be tested. */
public final class FactoryBalance {
    public String scalingMode = "LEGACY";
    public Map<String, Integer> capacityByTier = new LinkedHashMap<>(Map.of(
            "LV", 1, "MV", 2, "HV", 4, "EV", 8, "IV", 16, "LuV", 32, "ZPM", 64, "UV", 128));
    public boolean allowUnlimited = false;
    public int technicalOperationCap = 1048576;
    public boolean warmupEnabled = true;
    public boolean batchEnabled = true;
    public List<String> allowedRecipeTypes = new ArrayList<>();
    public int baseParallel = 64;
    public int baseThreads = 16;
    public double maxWarmup = 8;
    public double warmupTau = 60;
    public int overloadTime = 120;
    public int maxBatchMultiplier = 1000;
    private static final String[] TIERS = {"ULV", "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV"};

    public void validate() {
        if (!"LEGACY".equals(scalingMode) && !"SHARED_BUDGET".equals(scalingMode) && !"UNLIMITED".equals(scalingMode)) scalingMode = "LEGACY";
        technicalOperationCap = Math.clamp(technicalOperationCap, 1, 1048576);
        baseParallel = Math.max(1, baseParallel);
        baseThreads = Math.max(1, baseThreads);
        maxBatchMultiplier = Math.max(1, maxBatchMultiplier);
        overloadTime = Math.max(1, overloadTime);
        if (!Double.isFinite(maxWarmup) || maxWarmup < 1) maxWarmup = 8;
        if (!Double.isFinite(warmupTau) || warmupTau <= 0) warmupTau = 60;
        var defaults = new FactoryBalance();
        if (capacityByTier == null) capacityByTier = defaults.capacityByTier;
        defaults.capacityByTier.forEach(capacityByTier::putIfAbsent);
        capacityByTier.replaceAll((key, value) -> value == null ? 1 : Math.clamp(value, 1, technicalOperationCap));
        if (allowedRecipeTypes == null) allowedRecipeTypes = new ArrayList<>();
        allowedRecipeTypes.removeIf(java.util.Objects::isNull);
    }
    public boolean legacy() { return "LEGACY".equals(scalingMode); }
    public boolean sharedBudget() { return !legacy() && !("UNLIMITED".equals(scalingMode) && allowUnlimited); }
    public int capacity(int tier) {
        if (!sharedBudget()) return technicalOperationCap;
        String key = TIERS[Math.clamp(tier, 0, TIERS.length - 1)];
        return Math.clamp(capacityByTier.getOrDefault(key, 1), 1, technicalOperationCap);
    }
    public int threads(int tier, int selected) {
        if (!legacy()) return Math.clamp(selected, 1, Math.min(256, capacity(tier)));
        return (int) Math.min(Integer.MAX_VALUE, (long) baseThreads << Math.clamp(tier, 0, 20));
    }
    public double warmup(long seconds) {
        if (!warmupEnabled || seconds <= 0) return 1;
        if (seconds >= overloadTime) return maxWarmup;
        return 1 + (maxWarmup - 1) * (1 - Math.exp(-seconds / warmupTau));
    }
    public int parallel(int tier, int selected, int batch, long seconds) {
        if (!legacy()) return Math.max(1, capacity(tier) / threads(tier, selected));
        double value = baseParallel * Math.scalb(1.0, Math.clamp(tier, 0, 20))
                * (batchEnabled ? Math.clamp(batch, 1, maxBatchMultiplier) : 1) * warmup(seconds);
        return (int) Math.clamp(value, 1, Integer.MAX_VALUE);
    }
    public long remaining(int tier, long occupied) {
        return legacy() ? Long.MAX_VALUE : Math.max(0L, capacity(tier) - occupied);
    }
}
