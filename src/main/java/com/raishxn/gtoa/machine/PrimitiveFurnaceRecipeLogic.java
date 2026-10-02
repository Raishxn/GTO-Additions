package com.raishxn.gtoa.machine;

import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeDefinition;
import com.gregtechceu.gtceu.api.recipe.handler.RecipeHandlerUnit;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import com.gregtechceu.gtceu.utils.TaskHandler;


/**
 * Processes every distinct matching recipe/input unit in one server tick. Threads are logical
 * recipe lanes, never Java threads. No array or loop is sized to Integer.MAX_VALUE.
 * Inputs and outputs are committed together, so no additional in-flight inventory needs saving.
 */
final class PrimitiveFurnaceRecipeLogic extends RecipeLogic {
    private final RecipeLaneTracker<RecipeHandlerUnit, ResourceLocation> processed = new RecipeLaneTracker<>();

    PrimitiveFurnaceRecipeLogic(PrimitiveStoneFurnaceMachine machine) {
        super(machine);
        interval = 1;
    }

    @Override
    public void updateTickSubscription() {
        if (status == SUSPEND || !machine.isRecipeLogicAvailable()) {
            unsubscribe();
            return;
        }
        if ((subscription == null || !subscription.stillSubscribed)
                && machine.self().getLevel() instanceof ServerLevel level) {
            // Reuse GTO's execution monitor, but wake on the next tick instead of waiting five.
            subscription = TaskHandler.enqueueTick(level, machine.self().holder.isRemove, monitor, 1, 0);
        }
    }

    @Override
    public void serverTick() {
        if (status == SUSPEND || !machine.isRecipeLogicAvailable()) {
            unsubscribe();
            return;
        }
        processed.clear();
        boolean worked = false;
        // Each successful search adds a new pair. This terminates after the finite set of
        // matching recipes/input units, even for non-consumable or self-reproducing recipes.
        while (machine.findRecipe(machine.getRecipeType(), this, lockedRecipe)) {
            worked = true;
            if (suspendAfterFinish) {
                suspendAfterFinish = false;
                setStatus(SUSPEND);
                break;
            }
        }
        processed.clear();
        isActive = worked;
        if (!worked) {
            lastRecipe = null;
            duration = 0;
            progress = 0;
        }
        if (status != SUSPEND) setStatus(worked ? WORKING : IDLE);
        interval = 1;
        if (subscription != null) subscription.cycle = 1;
        if (!worked && !machine.keepSubscribing()) unsubscribe();
    }

    @Override
    public boolean test(RecipeHandlerUnit unit, GTRecipeDefinition definition) {
        if (processed.contains(unit, definition.id)) return false;
        return super.test(unit, definition);
    }

    @Override
    public boolean setupRecipe(RecipeHandlerUnit unit, GTRecipe recipe) {
        if (!machine.handleRecipeInput(unit, recipe)) return false;
        processed.add(unit, recipe.definition.id);
        machine.beforeWorking(unit, recipe);
        machine.onWorking();
        machine.handleRecipeOutput(recipe);
        machine.afterWorking();
        lastRecipe = recipe;
        duration = 1;
        progress = 1;
        totalContinuousRunningTime++;
        return true;
    }

    @Override
    public void resetRecipeLogic() {
        processed.clear();
        super.resetRecipeLogic();
        interval = 1;
    }
}
