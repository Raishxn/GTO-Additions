package com.raishxn.gtoa.machine;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.recipe.handler.IO;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gto.datasynclib.annotations.SaveToDisk;
import com.gto.datasynclib.annotations.SyncToClient;
import com.raishxn.gtoa.recipe.BoostedFluidHandler;
import com.raishxn.gtoa.recipe.OutputMultiplier;
import com.raishxn.gtoa.gui.IntegerConfigurator;

public class ExtendedFluidHatchMachine extends FluidHatchPartMachine {
    @SaveToDisk @SyncToClient private int outputBoost = 1;
    private final boolean steam;
    public ExtendedFluidHatchMachine(MetaMachineBlockEntity holder, int tier, IO io, int initial, int slots, boolean steam) {
        super(holder, tier, io, initial, slots);
        this.steam = steam;
        outputBoost = getMaxBoost();
    }
    @Override protected NotifiableFluidTank createTank(int initial, int slots, Object... args) {
        int capacity = Math.toIntExact((long) getTankCapacity(initial, getTier()) * 8);
        return new BoostedFluidHandler(this, slots, capacity, io, this::getOutputBoost);
    }
    public int getMaxBoost() { return steam ? 8 : OutputMultiplier.limit(getTier()); }
    public int getOutputBoost() { return Math.clamp(outputBoost, 1, getMaxBoost()); }
    public void setOutputBoost(int value) {
        if (isRemote()) return;
        for (var controller : getControllers()) {
            if (controller instanceof com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine recipeMachine
                    && recipeMachine.isActive()) return;
        }
        outputBoost = Math.clamp(value, 1, getMaxBoost());
        onChanged(); requestSync();
    }
    @Override public void attachConfigurators(ConfiguratorPanel panel) {
        super.attachConfigurators(panel);
        if (io == IO.OUT) panel.attachConfigurators(new IntegerConfigurator("gtoa.output_boost", this::getOutputBoost,
                this::setOutputBoost, 1, getMaxBoost()));
    }
    @Override public boolean swapIO() { return false; }
}
