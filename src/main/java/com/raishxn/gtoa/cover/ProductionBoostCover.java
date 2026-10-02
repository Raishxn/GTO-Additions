package com.raishxn.gtoa.cover;

import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator;
import com.gto.datasynclib.annotations.SaveToDisk;
import com.gto.datasynclib.annotations.SyncToClient;
import com.raishxn.gtoa.gui.IntegerConfigurator;
import com.raishxn.gtoa.recipe.OutputMultiplier;
import net.minecraft.core.Direction;

public final class ProductionBoostCover extends CoverBehavior implements com.gregtechceu.gtceu.api.cover.IUICover {
    private final int tier;
    @SaveToDisk @SyncToClient private int outputBoost;
    public ProductionBoostCover(CoverDefinition definition, ICoverable holder, Direction side, int tier) {
        super(definition, holder, side);
        this.tier = tier;
        outputBoost = OutputMultiplier.limit(tier);
    }
    public static boolean accepts(MetaMachine machine) {
        return machine instanceof IRecipeLogicMachine && !(machine instanceof IMultiController) && !(machine instanceof IMultiPart);
    }
    @Override public boolean canAttach() { return accepts(MetaMachine.getMachine(coverHolder.holder())); }
    public int getOutputBoost() { return Math.clamp(outputBoost, 1, OutputMultiplier.limit(tier)); }
    private void setOutputBoost(int value) {
        if (coverHolder.isRemote()) return;
        outputBoost = Math.clamp(value, 1, OutputMultiplier.limit(tier));
        coverHolder.onChanged();
        scheduleUpdate(com.gto.datasynclib.LogicalSide.CLIENT);
    }
    @Override public com.lowdragmc.lowdraglib.gui.widget.Widget createUIWidget() {
        return getConfigurator().createConfigurator();
    }
    @Override public IFancyConfigurator getConfigurator() {
        return new IntegerConfigurator("gtoa.output_boost", this::getOutputBoost, this::setOutputBoost, 1, OutputMultiplier.limit(tier));
    }
}
