package com.raishxn.gtoa.recipe;

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.raishxn.gtoa.cover.ProductionBoostCover;

public final class CoverRecipeBoost {
    private CoverRecipeBoost() {}
    /** Called after fullModifyRecipe and before recipe matching, on the runtime copy. */
    public static GTRecipe modify(GTRecipe recipe, IRecipeLogicMachine machine) {
        if (recipe == null || !ProductionBoostCover.accepts(machine.self())) return recipe;
        int boost = 0;
        for (var cover : machine.self().getCoverContainer().getCovers()) {
            if (cover instanceof ProductionBoostCover production) boost = Math.max(boost, production.getOutputBoost());
        }
        if (boost == 0) return recipe;
        try {
            var items = OutputMultiplier.copy(recipe.itemOutputs, boost);
            var fluids = OutputMultiplier.copy(recipe.fluidOutputs, boost);
            recipe.itemOutputs = items;
            recipe.fluidOutputs = fluids;
            recipe.duration = BoostMath.duration(recipe.duration);
            recipe.eut = BoostMath.energy(recipe.eut);
            return recipe;
        } catch (ArithmeticException overflow) { return null; }
    }
}
