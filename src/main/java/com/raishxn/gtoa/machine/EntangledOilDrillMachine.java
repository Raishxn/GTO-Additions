package com.raishxn.gtoa.machine;

import com.raishxn.gtoa.GTOAItems;
import com.raishxn.gtoa.mining.FluidFieldLink;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.recipe.GTRecipeDefinition;
import com.gregtechceu.gtceu.api.recipe.handler.RecipeHandlerUnit;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

public final class EntangledOilDrillMachine extends EntangledMachine {
    public EntangledOilDrillMachine(MetaMachineBlockEntity holder, int tier, boolean steam) { super(holder, tier, steam, true); }
    @Override protected boolean acceptsCard(ItemStack stack) { return stack.is(GTOAItems.ENTANGLED_FLUID_CARD.get()); }
    @Override public GTRecipeDefinition createCustomRecipe(RecipeHandlerUnit unit) {
        ItemStack[] card = {ItemStack.EMPTY};
        unit.forEachItems(true, (stack, count) -> {
            if (acceptsCard(stack) && FluidFieldLink.read(stack) != null) { card[0] = stack.copyWithCount(1); return true; }
            return false;
        });
        var link = FluidFieldLink.read(card[0]);
        if (link == null) { setIdleReason(Component.translatable("gtoa.oil.missing_card")); return null; }
        var definition = GTRegistries.BEDROCK_FLUID_DEFINITIONS.get(link.field());
        if (definition == null) { setIdleReason(Component.translatable("gtoa.oil.invalid_field")); return null; }
        var fluid = definition.getStoredFluid().get();
        if (fluid == Fluids.EMPTY || !link.fluid().equals(BuiltInRegistries.FLUID.getKey(fluid))) {
            setIdleReason(Component.translatable("gtoa.oil.invalid_field")); return null;
        }
        int amount;
        try { amount = EntangledRates.fluidPerCycle(link.yield(), level()); }
        catch (ArithmeticException overflow) { setIdleReason(Component.translatable("gtoa.oil.invalid_field")); return null; }
        return cycleBuilder(ResourceLocation.fromNamespaceAndPath("gtoa", "entangled_oil/" + link.field().getNamespace()
                + "/" + link.field().getPath()), card[0]).outputFluids(new FluidStack(fluid, amount)).build();
    }
}
