package com.raishxn.gtoa.recipe;

import com.gregtechceu.gtceu.api.transfer.fluid.ICustomFluidStackHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public final class DistillationOutputBoost {
    private DistillationOutputBoost() {}
    public static int fillInternal(ICustomFluidStackHandler handler, FluidStack fluid, IFluidHandler.FluidAction action) {
        if (handler instanceof BoostedFluidHandler boosted) return boosted.fillRecipe(fluid, action);
        return handler.fillInternal(fluid, action);
    }
    public static int fill(IFluidHandler handler, FluidStack fluid, IFluidHandler.FluidAction action) {
        if (handler instanceof BoostedFluidHandler boosted) return boosted.fillRecipe(fluid, action);
        return handler.fill(fluid, action);
    }
}
