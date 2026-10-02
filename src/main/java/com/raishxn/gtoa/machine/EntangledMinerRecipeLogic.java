package com.raishxn.gtoa.machine;

import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import net.minecraft.network.chat.Component;

/** Holds a finished batch until all its multiplied outputs fit, without charging more energy. */
public final class EntangledMinerRecipeLogic extends RecipeLogic {
    public EntangledMinerRecipeLogic(EntangledMachine machine) { super(machine); }
    @Override public boolean onRecipeFinish() {
        if (lastRecipe != null && !machine.matchRecipeOutput(lastRecipe)) {
            setWaiting(Component.translatable("gtoa.entangled.output_full"));
            // In RecipeLogic.serverTick, true preserves the completed progress and pending recipe.
            return true;
        }
        return super.onRecipeFinish();
    }
}
