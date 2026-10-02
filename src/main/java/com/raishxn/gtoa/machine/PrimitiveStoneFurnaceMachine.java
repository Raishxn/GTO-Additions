package com.raishxn.gtoa.machine;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeDefinition;
import com.gregtechceu.gtceu.api.recipe.handler.RecipeHandlerUnit;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gtolib.api.machine.multiblock.NoEnergyMultiblockMachine;

/** GTNA furnace concept adapted to the GTO dev9 recipe/handler API. */
public final class PrimitiveStoneFurnaceMachine extends NoEnergyMultiblockMachine {
    public PrimitiveStoneFurnaceMachine(MetaMachineBlockEntity holder) { super(holder); }

    @Override
    public RecipeLogic createRecipeLogic(Object... args) {
        return new PrimitiveFurnaceRecipeLogic(this);
    }

    @Override
    public GTRecipe fullModifyRecipe(RecipeHandlerUnit unit, GTRecipeDefinition definition) {
        // Always use a fresh runtime recipe: shared definitions must retain their EU/t and duration.
        GTRecipe recipe = definition.toRuntime();
        recipe.setEUt(0);
        recipe.duration = 1;
        if (unit.color != -1) recipe.outputColor = unit.color;
        return ParallelLogic.accurateParallel(this, unit, recipe, ParallelLogic.MAX_PARALLEL);
    }
}
