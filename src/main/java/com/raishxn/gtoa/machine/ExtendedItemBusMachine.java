package com.raishxn.gtoa.machine;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.recipe.handler.IO;
import com.gregtechceu.gtceu.common.machine.multiblock.part.ItemBusPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gto.datasynclib.annotations.SaveToDisk;
import com.gto.datasynclib.annotations.SyncToClient;
import com.raishxn.gtoa.recipe.BoostedItemHandler;
import com.raishxn.gtoa.recipe.OutputMultiplier;
import com.raishxn.gtoa.gui.IntegerConfigurator;

public class ExtendedItemBusMachine extends ItemBusPartMachine {
    @SaveToDisk @SyncToClient private int outputBoost = 1;
    private final boolean steam;
    public ExtendedItemBusMachine(MetaMachineBlockEntity holder, int tier, IO io, boolean steam) {
        super(holder, tier, io);
        this.steam = steam;
        outputBoost = getMaxBoost();
    }
    @Override protected int getInventorySize() { return super.getInventorySize() * 8; }
    @Override protected NotifiableItemStackHandler createInventory(Object... args) {
        return new BoostedItemHandler(this, getInventorySize(), io, this::getOutputBoost);
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
