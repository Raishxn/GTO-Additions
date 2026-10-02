package com.raishxn.gtoa.recipe;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.handler.IO;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient;
import java.util.List;
import java.util.function.IntSupplier;

/** Multiplies recipe outputs only; ordinary capability insertion never creates resources. */
public final class BoostedFluidHandler extends NotifiableFluidTank {
    private final IntSupplier multiplier;
    public BoostedFluidHandler(MetaMachine machine, int slots, int capacity, IO io, IntSupplier multiplier) {
        super(machine, slots, capacity, io);
        this.multiplier = multiplier;
    }
    public int fillRecipe(net.minecraftforge.fluids.FluidStack fluid,
                          net.minecraftforge.fluids.capability.IFluidHandler.FluidAction action) {
        try {
            var boosted = fluid.copy();
            boosted.setAmount(Math.toIntExact(OutputMultiplier.amount(fluid.getAmount(), multiplier.getAsInt())));
            if (super.fillInternal(boosted, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE) != boosted.getAmount()) return 0;
            if (action.execute()) super.fillInternal(boosted, action);
            return fluid.getAmount();
        } catch (ArithmeticException overflow) { return 0; }
    }
    @Override
    public boolean handleRecipeFluid(IO io, GTRecipe recipe, List<Content<FluidIngredient>> contents, boolean simulate) {
        int boost = multiplier.getAsInt();
        if (io != IO.OUT || boost == 1) return super.handleRecipeFluid(io, recipe, contents, simulate);
        try {
            // Probe the entire boosted remainder before committing to prevent partial writes and retries.
            var probe = OutputMultiplier.copy(contents, boost);
            super.handleRecipeFluid(io, recipe, probe, true);
            if (!probe.isEmpty()) return false;
            if (!simulate) {
                var output = OutputMultiplier.copy(contents, boost);
                super.handleRecipeFluid(io, recipe, output, false);
                if (!output.isEmpty()) throw new IllegalStateException("Output changed after simulation");
            }
            contents.clear();
            return true;
        } catch (ArithmeticException overflow) {
            return false;
        }
    }
}
