package com.raishxn.gtoa.factory;

import com.gregtechceu.gtceu.api.machine.fancyconfigurator.MachineModeFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.Widget;

/** Keep the native mode synchronization/buttons inside a bounded scroll viewport. */
public final class FactoryModesPage extends MachineModeFancyConfigurator {
    public FactoryModesPage(IRecipeLogicMachine machine) { super(machine); }
    @Override public Widget createMainPage(FancyMachineUIWidget widget) {
        var scroll = new DraggableScrollableWidgetGroup(0, 0, 150, 180);
        scroll.setDraggable(false).setScrollable(true).setUseScissor(true);
        scroll.addWidget(super.createMainPage(widget));
        return scroll;
    }
}
