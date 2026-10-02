package com.raishxn.gtoa.machine;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.blockentity.ITickSubscription;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.handler.RecipeHandlerUnit;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gregtechceu.gtceu.api.gui.fancy.TabsWidget;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.gto.datasynclib.annotations.SaveToDisk;
import com.gto.datasynclib.annotations.SyncToClient;
import com.raishxn.gtoa.factory.FactoryConfig;
import com.raishxn.gtoa.factory.FactoryBalance;
import com.raishxn.gtoa.factory.FactoryModesPage;
import com.raishxn.gtoa.gui.IntegerConfigurator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import java.util.List;
import java.util.Locale;

/** GTNA Universal Factory scaling adapted to GTO recipe units and native handlers. */
public final class UniversalFactoryMachine extends WorkableElectricMultiblockMachine {
    @SaveToDisk @SyncToClient private int selectedThreads = 1;
    @SaveToDisk @SyncToClient private int batchMultiplier = 1;
    @SaveToDisk @SyncToClient private long runningSecs;
    @SyncToClient private int liveThreads;
    @SyncToClient private long liveParallel;
    @SyncToClient private int effectiveThreadLimit = 1;
    @SyncToClient private int effectiveParallelLimit = 1;
    @SyncToClient private int effectiveCapacity = 1;
    @SyncToClient private int thermalThreshold = 120;
    @SyncToClient private double effectiveWarmup = 1;
    @SyncToClient private String scalingMode = "LEGACY";
    private TickableSubscription thermal;

    public UniversalFactoryMachine(MetaMachineBlockEntity holder) { super(holder); }
    public FactoryBalance balance() { return FactoryConfig.get(); }
    public int operatingTier() { return Math.clamp(GTUtil.getTierByVoltage(getOverclockVoltage()), 0, 20); }
    public int threadLimit() { return balance().threads(operatingTier(), selectedThreads); }
    public int parallelLimit() { return balance().parallel(operatingTier(), selectedThreads, batchMultiplier, runningSecs); }
    public long remainingBudget() { return balance().remaining(operatingTier(), logic().occupied()); }
    private UniversalFactoryRecipeLogic logic() { return (UniversalFactoryRecipeLogic) recipeLogic; }
    @Override public RecipeLogic createRecipeLogic(Object... args) { return new UniversalFactoryRecipeLogic(this); }

    @Override protected GTRecipe getRealRecipe(RecipeHandlerUnit unit, GTRecipe recipe) {
        long limit = Math.min(parallelLimit(), remainingBudget());
        if (limit < 1 || recipe.eut < 0 || recipe.parallels > limit) return null;
        recipe = ParallelLogic.accurateParallel(this, unit, recipe, limit / Math.max(1, recipe.parallels));
        if (recipe == null) return null;
        // Classic 4x EU / 2x speed overclock after parallelizing, as in GTNA.
        // No sub-tick parallel multiplication: shared-budget accounting stays exact.
        long voltage = getOverclockVoltage();
        while (recipe.eut > 0 && recipe.eut <= voltage / 4 && recipe.duration > 1) {
            recipe.setEUt(recipe.eut * 4);
            recipe.duration = Math.max(1, recipe.duration / 2);
            recipe.ocLevel++;
        }
        return recipe;
    }
    @Override public boolean hasBatchConfig() { return false; }
    @Override public boolean isBatchEnabled() { return false; }
    @Override public boolean alwaysSearchRecipe() { return true; }
    // GTO only wires content-change wakeups for its enhanced multiblock interface.
    // This factory owns its lanes, so keep polling even after the last lane completes.
    @Override public boolean keepSubscribing() { return true; }
    @Override public boolean supportLockRecipe() { return false; }

    @Override public void onLoad() {
        super.onLoad();
        if (!isRemote()) thermal = subscribeServerTick(thermal, this::updateHeat, 20);
    }
    private void updateHeat() {
        if (isFormed() && logic().isWorking()) runningSecs = runningSecs == Long.MAX_VALUE ? runningSecs : runningSecs + 1;
        else runningSecs = Math.max(0, runningSecs - 16);
        updateCapacityDisplay();
        onChanged();
        requestSync();
    }
    private void updateCapacityDisplay() {
        effectiveThreadLimit = threadLimit();
        effectiveParallelLimit = parallelLimit();
        effectiveCapacity = balance().capacity(operatingTier());
        effectiveWarmup = balance().warmup(runningSecs);
        thermalThreshold = balance().overloadTime;
        scalingMode = balance().scalingMode;
    }
    @Override public void onUnload() {
        thermal = ITickSubscription.unsubscribe(thermal);
        super.onUnload();
    }
    @Override public void onWorking() {
        if (runningSecs == 0) runningSecs = 1;
        super.onWorking();
    }
    void syncLanes(int threads, long parallel) {
        if (!isRemote()) updateCapacityDisplay();
        if (liveThreads != threads || liveParallel != parallel) {
            liveThreads = threads;
            liveParallel = parallel;
            requestSync();
        }
    }
    @Override public void saveCustomPersistedData(CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
        tag.put("UniversalFactoryLanes", logic().saveLanes());
    }
    @Override public void loadCustomPersistedData(CompoundTag tag) {
        super.loadCustomPersistedData(tag);
        logic().loadLanes(tag.getCompound("UniversalFactoryLanes"));
    }
    @Override public void attachSideTabs(TabsWidget tabs) {
        tabs.setMainTab(this);
        tabs.attachSubTab(new FactoryModesPage(this));
    }
    @Override public void attachConfigurators(ConfiguratorPanel panel) {
        super.attachConfigurators(panel);
        panel.attachConfigurators(new IntegerConfigurator("gtoa.factory.threads", () -> selectedThreads,
                value -> {
                    if (!isRemote() && !isActive()) {
                        selectedThreads = Math.clamp(value, 1, Math.min(256, balance().capacity(operatingTier())));
                        onChanged(); requestSync();
                    }
                }, 1, 256));
        panel.attachConfigurators(new IntegerConfigurator("gtoa.factory.batch", () -> batchMultiplier,
                value -> {
                    if (!isRemote() && !isActive()) {
                        batchMultiplier = Math.clamp(value, 1, balance().maxBatchMultiplier);
                        onChanged(); requestSync();
                    }
                }, 1, balance().maxBatchMultiplier));
    }
    @Override public void addDisplayText(List<Component> text) {
        super.addDisplayText(text);
        if (!isFormed()) return;
        text.add(Component.translatable("gtoa.factory.mode", scalingMode));
        text.add(Component.translatable("gtoa.factory.capacity", liveThreads, effectiveThreadLimit, liveParallel, effectiveParallelLimit));
        if ("LEGACY".equals(scalingMode)) {
            text.add(Component.translatable("gtoa.factory.heat", runningSecs, thermalThreshold,
                    String.format(Locale.ROOT, "%.2f", effectiveWarmup)));
            text.add(Component.translatable("gtoa.factory.batch_status", batchMultiplier));
        } else text.add(Component.translatable("gtoa.factory.budget", effectiveCapacity));
        text.add(Component.translatable("gtoa.factory.automatic"));
    }
}
