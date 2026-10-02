package com.raishxn.gtoa.recipe;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.handler.IO;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.ingredient.ItemIngredient;
import java.util.List;
import java.util.function.IntSupplier;

/** Multiplies recipe outputs only; ordinary capability insertion never creates resources. */
public final class BoostedItemHandler extends NotifiableItemStackHandler {
    private final IntSupplier multiplier;
    public BoostedItemHandler(MetaMachine machine, int slots, IO io, IntSupplier multiplier) {
        super(machine, slots, io);
        this.multiplier = multiplier;
    }
    @Override
    public boolean handleRecipeItem(IO io, GTRecipe recipe, List<Content<ItemIngredient>> contents, boolean simulate) {
        int boost = multiplier.getAsInt();
        if (io != IO.OUT || boost == 1) return super.handleRecipeItem(io, recipe, contents, simulate);
        try {
            // Probe the entire boosted remainder before committing to prevent partial writes and retries.
            var probe = OutputMultiplier.copy(contents, boost);
            super.handleRecipeItem(io, recipe, probe, true);
            if (!probe.isEmpty()) return false;
            if (!simulate) {
                var output = OutputMultiplier.copy(contents, boost);
                super.handleRecipeItem(io, recipe, output, false);
                if (!output.isEmpty()) throw new IllegalStateException("Output changed after simulation");
            }
            contents.clear();
            return true;
        } catch (ArithmeticException overflow) {
            return false;
        }
    }
}
