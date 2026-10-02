package com.raishxn.gtoa.machine;

import com.gregtechceu.gtceu.api.gui.editor.EditableMachineUI;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.handler.IO;
import com.gregtechceu.gtceu.api.recipe.info.ItemRecipeInfo;
import com.gregtechceu.gtceu.api.recipe.info.FluidRecipeInfo;
import com.gregtechceu.gtceu.api.recipe.ui.GTRecipeTypeUI;
import com.gto.datasynclib.datastream.DataComponentMap;
import com.google.common.collect.HashBasedTable;
import java.util.Collections;

public final class EntangledMachineUI {
    private EntangledMachineUI() {}
    public static EditableMachineUI steam(GTRecipeType type) {
        return new EditableMachineUI("simple", type.registryName,
                () -> type.getRecipeUI().createEditableUITemplate(true, false).createDefault(), (template, machine) -> {
                    var miner = (EntangledMachine) machine;
                    var storages = HashBasedTable.<IO, com.gregtechceu.gtceu.api.recipe.info.RecipeInfo, Object>create();
                    storages.put(IO.IN, ItemRecipeInfo.INSTANCE, miner.importItems.storage);
                    storages.put(IO.OUT, ItemRecipeInfo.INSTANCE, miner.exportItems.storage);
                    storages.put(IO.OUT, FluidRecipeInfo.INSTANCE, miner.exportFluids);
                    type.getRecipeUI().createEditableUITemplate(true, false).setupUI(template,
                            new GTRecipeTypeUI.RecipeHolder(miner.recipeLogic::getProgressPercent, storages,
                                    new DataComponentMap(), Collections.emptyList(), true, false));
                });
    }
}
