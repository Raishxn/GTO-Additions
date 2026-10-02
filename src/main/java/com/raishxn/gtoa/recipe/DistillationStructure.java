package com.raishxn.gtoa.recipe;

import com.gtocore.common.data.GTOMachines;
import com.raishxn.gtoa.GTOAMachines;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import java.util.Arrays;

public final class DistillationStructure {
    private DistillationStructure() {}
    public static MetaMachineBlock[] withThermostat(MetaMachineBlock[] blocks) {
        for (MetaMachineBlock block : blocks) {
            if (block == GTOMachines.HEAT_HATCH.get()) {
                MetaMachineBlock[] extended = Arrays.copyOf(blocks, blocks.length + 1);
                extended[blocks.length] = GTOAMachines.DISTILLATION_THERMOSTAT_HATCH.get();
                return extended;
            }
        }
        return blocks;
    }
}
