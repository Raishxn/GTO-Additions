package com.raishxn.gtoa.machine;

import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeDefinition;
import com.gregtechceu.gtceu.api.recipe.handler.RecipeHandlerUnit;
import com.gregtechceu.gtceu.utils.TaskHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.CompoundTag;
import com.gto.datasynclib.datastream.data.Data;
import com.gregtechceu.gtceu.datasynclib.GTDataFixer;
import com.raishxn.gtoa.factory.FactoryWorkQueue;

/** Logical recipe lanes, all executed on the server tick, never Java threads. */
final class UniversalFactoryRecipeLogic extends RecipeLogic {
    private final UniversalFactoryMachine factory;
    private final FactoryWorkQueue<GTRecipe> lanes = new FactoryWorkQueue<>();
    private final RecipeLaneTracker<RecipeHandlerUnit, ResourceLocation> started = new RecipeLaneTracker<>();
    private int nextType;
    private boolean draining;
    UniversalFactoryRecipeLogic(UniversalFactoryMachine factory) { super(factory); this.factory = factory; interval = 1; }
    long occupied() { return lanes.occupied(); }
    @Override public void updateTickSubscription() {
        if (status == SUSPEND || !factory.isRecipeLogicAvailable()) { unsubscribe(); return; }
        if ((subscription == null || !subscription.stillSubscribed) && factory.getLevel() instanceof ServerLevel level) {
            subscription = TaskHandler.enqueueTick(level, factory.holder.isRemove, monitor, 1, 0);
        }
    }
    @Override public void serverTick() {
        if (status == SUSPEND || !factory.isRecipeLogicAvailable()) { unsubscribe(); return; }
        markLastRecipeDirty();
        boolean worked = lanes.tick(recipe -> {
            if (!factory.matchTickRecipe(recipe) || !factory.handleTickRecipe(recipe)) return false;
            factory.onWorking();
            return true;
        }, factory::matchRecipeOutput, recipe -> {
            // A successful preflight owns exactly one commit attempt; never replay partial outputs.
            if (!factory.handleRecipeOutput(recipe))
                com.mojang.logging.LogUtils.getLogger().error("Universal Factory output handler failed after successful simulation: {}", recipe.definition.id);
            factory.afterWorking();
        });
        if (suspendAfterFinish) { draining = true; suspendAfterFinish = false; }
        if (!draining) {
            started.clear();
            var types = factory.getAvailableRecipeTypes();
            for (int i = 0; i < types.length && lanes.size() < factory.threadLimit() && factory.remainingBudget() > 0; i++) {
                int index = (nextType + i) % types.length;
                var type = types[index];
                var allowed = factory.balance().allowedRecipeTypes;
                if (!allowed.isEmpty() && !allowed.contains(type.registryName.toString())) continue;
                // Use native input-unit search; test() additionally validates encoded outputs.
                while (lanes.size() < factory.threadLimit() && factory.remainingBudget() > 0
                        && factory.findRecipe(type, this, lockedRecipe)) worked = true;
            }
            if (types.length > 0) nextType = (nextType + 1) % types.length;
            started.clear();
        }
        if (!lanes.isEmpty()) {
            var lane = lanes.entries().getFirst();
            lastRecipe = lane.recipe;
            progress = lane.progress;
            duration = lane.duration;
        } else { lastRecipe = null; progress = 0; duration = 0; }
        isActive = !lanes.isEmpty();
        factory.syncLanes(lanes.size(), occupied());
        factory.onChanged();
        if (draining && lanes.isEmpty()) { draining = false; setStatus(SUSPEND); }
        // Completing the last lane is work this tick, but leaves an idle machine.
        // WORKING with no lanes would retain sound/heat after unsubscribing.
        else setStatus(lanes.isEmpty() ? IDLE : worked ? WORKING : WAITING);
        if (subscription != null) subscription.cycle = 1;
        if (lanes.isEmpty() && !factory.keepSubscribing()) unsubscribe();
    }
    @Override public boolean test(RecipeHandlerUnit unit, GTRecipeDefinition definition) {
        var allowed = factory.balance().allowedRecipeTypes;
        // GTNA permits only one active lane per recipe when parallel/thread exceeds one.
        if (factory.parallelLimit() > 1 && lanes.entries().stream()
                .anyMatch(lane -> lane.recipe.definition.id.equals(definition.id))) return false;
        if (started.contains(unit, definition.id)
                || (!allowed.isEmpty() && !allowed.contains(definition.recipeType.registryName.toString()))
                || !com.raishxn.gtoa.factory.FactoryPatternGuard.matches(unit, definition)) return false;
        return super.test(unit, definition);
    }
    @Override public boolean setupRecipe(RecipeHandlerUnit unit, GTRecipe recipe) {
        long operationLimit = factory.balance().legacy() ? Long.MAX_VALUE : factory.balance().capacity(factory.operatingTier());
        if (!lanes.canAccept(factory.threadLimit(), operationLimit, recipe.parallels)
                || !factory.handleRecipeInput(unit, recipe)) return false;
        factory.beforeWorking(unit, recipe);
        factory.setRecipeType(recipe.definition.recipeType);
        lanes.restore(new FactoryWorkQueue.Entry<>(recipe, Math.max(1, recipe.duration), recipe.parallels, 0));
        started.add(unit, recipe.definition.id);
        return true;
    }
    @Override public void resetRecipeLogic() {
        // Structure invalidation must pause committed inputs, not erase their pending outputs.
        started.clear();
        super.resetRecipeLogic();
        isActive = !lanes.isEmpty();
    }
    @Override public boolean isActive() { return super.isActive() || !lanes.isEmpty(); }
    CompoundTag saveLanes() {
        var result = new CompoundTag();
        var entries = new ListTag();
        for (var lane : lanes.entries()) {
            var tag = new CompoundTag();
            // Disk codec uses stable registry names, preserves output color and data version.
            tag.putByteArray("recipe", GTRecipe.DATA_CODEC.encode(lane.recipe).writeToBytes());
            tag.putInt("progress", lane.progress);
            entries.add(tag);
        }
        result.put("entries", entries);
        result.putInt("dataVersion", GTDataFixer.VERSION);
        result.putBoolean("draining", draining);
        result.putInt("nextType", nextType);
        return result;
    }
    void loadLanes(CompoundTag state) {
        lanes.clear();
        draining = state.getBoolean("draining");
        nextType = Math.max(0, state.getInt("nextType"));
        var tags = state.getList("entries", 10);
        for (int i = 0; i < tags.size(); i++) {
            var tag = tags.getCompound(i);
            var recipe = GTRecipe.DATA_CODEC.decode(Data.readData(tag.getByteArray("recipe")), state.getInt("dataVersion"));
            lanes.restore(new FactoryWorkQueue.Entry<>(recipe, Math.max(1, recipe.duration), recipe.parallels, tag.getInt("progress")));
        }
        isActive = !lanes.isEmpty();
        factory.syncLanes(lanes.size(), occupied());
    }
}
