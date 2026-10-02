package com.raishxn.gtoa.machine;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeDefinition;
import com.gregtechceu.gtceu.api.recipe.handler.ICustomRecipeLogicHolder;
import com.gregtechceu.gtceu.api.recipe.handler.RecipeHandlerUnit;
import com.gregtechceu.gtceu.api.recipe.handler.IO;
import com.gregtechceu.gtceu.api.GTValues;
import com.gto.datasynclib.annotations.SaveToDisk;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public abstract class EntangledMachine extends SimpleTieredMachine implements ICustomRecipeLogicHolder {
    protected final boolean steam;
    @SaveToDisk private int fuelTicks;
    @SaveToDisk protected long completedCycles;

    protected EntangledMachine(MetaMachineBlockEntity holder, int tier, boolean steam, boolean fluids) {
        super(holder, tier, t -> fluids ? 64000 << t : 0, steam);
        this.steam = steam;
    }
    @Override protected NotifiableEnergyContainer createEnergyContainer(Object... args) {
        // Constructors invoke this before the instance steam field is assigned.
        if (args.length > 0 && Boolean.TRUE.equals(args[0]))
            return new NotifiableEnergyContainer(this, 0, 0, 0, 0, 0);
        return super.createEnergyContainer(args);
    }
    @Override protected NotifiableItemStackHandler createImportItemHandler(Object... args) {
        boolean coalFired = args.length > 0 && Boolean.TRUE.equals(args[0]);
        return new NotifiableItemStackHandler(this, coalFired ? 2 : 1, IO.IN)
                .setFilter(stack -> acceptsCard(stack) || coalFired && isCoal(stack));
    }
    private static boolean isCoal(ItemStack stack) { return stack.is(Items.COAL) || stack.is(Items.CHARCOAL); }
    private int fuelSlot() {
        for (int slot = 0; slot < importItems.getSlots(); slot++)
            if (isCoal(importItems.getStackInSlot(slot))) return slot;
        return -1;
    }
    protected abstract boolean acceptsCard(ItemStack stack);
    protected int level() { return steam ? 0 : getTier(); }
    @Override public boolean matchTickRecipe(GTRecipe recipe) {
        if (steam && fuelTicks <= 0 && fuelSlot() < 0) {
            setIdleReason(Component.translatable("gtoa.entangled.missing_coal"));
            return false;
        }
        return super.matchTickRecipe(recipe);
    }
    @Override public boolean handleTickRecipe(GTRecipe recipe) {
        if (steam && fuelTicks <= 0 && fuelSlot() < 0) {
            setIdleReason(Component.translatable("gtoa.entangled.missing_coal"));
            return false;
        }
        if (!super.handleTickRecipe(recipe)) return false;
        if (steam) {
            if (fuelTicks <= 0) {
                int slot = fuelSlot();
                if (slot < 0 || importItems.extractItemInternal(slot, 1, false).isEmpty()) return false;
                fuelTicks = EntangledRates.COAL_BURN_TICKS;
            }
            fuelTicks--;
            onChanged();
        }
        return true;
    }
    @Override public RecipeLogic createRecipeLogic(Object... args) { return new EntangledMinerRecipeLogic(this); }
    @Override public boolean alwaysSearchRecipe() { return true; }
    @Override public boolean supportLockRecipe() { return false; }
    @Override public GTRecipe fullModifyRecipe(RecipeHandlerUnit unit, GTRecipeDefinition definition) {
        var recipe = definition.toRuntime();
        if (unit.color != -1) recipe.outputColor = unit.color;
        return recipe;
    }
    protected com.gregtechceu.gtceu.api.recipe.GTRecipeBuilder cycleBuilder(ResourceLocation id, ItemStack card) {
        // These definitions exist only for this cycle, outside the global GTO recipe-loading builder.
        var builder = new com.gregtechceu.gtceu.api.recipe.GTRecipeBuilder(id, getRecipeType())
                .notConsumable(card).duration(EntangledRates.CYCLE_TICKS);
        if (!steam) builder.EUt(GTValues.V[getTier()]);
        return builder;
    }
    @Override public void afterWorking() {
        super.afterWorking();
        completedCycles++;
        onChanged();
    }
}
