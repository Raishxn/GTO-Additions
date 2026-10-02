package com.raishxn.gtoa.gui;

import com.gregtechceu.gtceu.api.gui.fancy.IFancyConfigurator;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.function.Supplier;
import java.util.function.Consumer;

public record IntegerConfigurator(String title, Supplier<Integer> getter, Consumer<Integer> setter,
                                  int min, int max) implements IFancyConfigurator {
    public Component getTitle() { return Component.translatable(title); }
    public IGuiTexture getIcon() { return new ItemStackTexture(new ItemStack(Items.COMPARATOR)); }
    public Widget createConfigurator() {
        return new IntInputWidget(0, 0, 110, 20, getter, setter).setMin(min).setMax(max);
    }
}
