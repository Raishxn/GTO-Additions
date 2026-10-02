package com.raishxn.gtoa.machine;

import com.raishxn.gtoa.GTOAItems;
import com.raishxn.gtoa.mining.RawOrePool;
import com.raishxn.gtoa.mining.VeinLink;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.recipe.GTRecipeDefinition;
import com.gregtechceu.gtceu.api.recipe.handler.RecipeHandlerUnit;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class EntangledMinerMachine extends EntangledMachine {
    public EntangledMinerMachine(MetaMachineBlockEntity holder, int tier, boolean steam) { super(holder, tier, steam, false); }
    @Override protected boolean acceptsCard(ItemStack stack) { return stack.is(GTOAItems.ENTANGLED_VEIN_CARD.get()); }
    public int rawPerCycle() { return EntangledRates.rawPerCycle(level()); }
    @Override public GTRecipeDefinition createCustomRecipe(RecipeHandlerUnit unit) {
        ItemStack[] card = {ItemStack.EMPTY};
        unit.forEachItems(true, (stack, count) -> {
            if (stack.is(GTOAItems.ENTANGLED_VEIN_CARD.get()) && VeinLink.read(stack) != null) {
                card[0] = stack.copyWithCount(1); return true;
            }
            return false;
        });
        var link = VeinLink.read(card[0]);
        if (link == null) { setIdleReason(Component.translatable("gtoa.entangled.missing_card")); return null; }
        var definition = GTRegistries.ORE_VEINS.get(link.vein());
        if (definition == null) { setIdleReason(Component.translatable("gtoa.entangled.invalid_deposit")); return null; }
        var pool = RawOrePool.from(definition);
        if (pool.isEmpty()) { setIdleReason(Component.translatable("gtoa.entangled.card.no_raw")); return null; }
        var output = pool.output(link, completedCycles, rawPerCycle());
        return cycleBuilder(ResourceLocation.fromNamespaceAndPath("gtoa", "entangled/" + link.vein().getNamespace() + "/" + link.vein().getPath()), card[0]).outputItems(output).duration(EntangledRates.MINER_CYCLE_TICKS).build();
    }
}
