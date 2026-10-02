package com.raishxn.gtoa.machine;

import com.gtocore.common.machine.multiblock.part.HeatHatchPartMachine;
import com.gtocore.common.machine.multiblock.noenergy.PrimitiveDistillationTowerMachine;
import com.gtocore.api.machine.part.IHeatContainerPart;
import com.gtocore.api.pattern.GTOPredicates;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.blockentity.ITickSubscription;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.gui.fancy.ConfiguratorPanel;
import com.gto.datasynclib.annotations.SaveToDisk;
import com.gto.datasynclib.annotations.SyncToClient;
import com.raishxn.gtoa.gui.IntegerConfigurator;
import java.util.Collections;

/** One thermostat freely regulates both heat containers of its primitive distillation tower. */
public final class DistillationThermostatHatchMachine extends HeatHatchPartMachine {
    @SaveToDisk @SyncToClient private int hotTemperature = 800;
    private TickableSubscription thermostat;
    public DistillationThermostatHatchMachine(MetaMachineBlockEntity holder) { super(holder, 10000, 100, 0); }
    @Override public void onLoad() {
        super.onLoad();
        if (!isRemote()) thermostat = subscribeServerTick(thermostat, this::regulate);
    }
    @Override public void onUnload() {
        thermostat = ITickSubscription.unsubscribe(thermostat);
        super.onUnload();
    }
    private void regulate() {
        for (var controller : getControllers()) {
            if (!(controller instanceof PrimitiveDistillationTowerMachine tower) || !tower.isFormed()) continue;
            var coldPositions = tower.getMultiblockState().getMatchContext().getOrDefault(GTOPredicates.DataKeys.A, Collections.emptySet());
            for (var part : tower.getParts()) {
                if (!(part instanceof IHeatContainerPart heatPart)) continue;
                var heat = heatPart.getHeatContainer();
                double target = coldPositions.contains(part.self().getPos()) ? 350 : Math.clamp(hotTemperature, 400, 2000);
                target = Math.min(target, heat.getMaxTemperature() - 1);
                long desiredHeat = Math.clamp(heat.getCurrentHeat() + (long) ((target - heat.getTemperature()) * heat.getHeatCapacity()), 0L, heat.getMaxHeat());
                if (desiredHeat != heat.getCurrentHeat()) heat.setCurrentHeat(desiredHeat);
            }
        }
    }
    @Override public boolean canShared() { return false; }
    @Override public void attachConfigurators(ConfiguratorPanel panel) {
        super.attachConfigurators(panel);
        panel.attachConfigurators(new IntegerConfigurator("gtoa.distillation.hot_temperature", () -> hotTemperature,
                value -> { if (!isRemote()) { hotTemperature = Math.clamp(value, 400, 2000); onChanged(); requestSync(); } }, 400, 2000));
    }
}
