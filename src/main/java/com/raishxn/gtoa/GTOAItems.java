package com.raishxn.gtoa;

import com.raishxn.gtoa.item.PrimitiveFurnaceKitItem;
import com.raishxn.gtoa.item.EntangledVeinCardItem;
import com.raishxn.gtoa.item.EntangledFluidCardItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class GTOAItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GTOAdditions.MOD_ID);
    public static final RegistryObject<Item> PRIMITIVE_FURNACE_KIT = ITEMS.register("primitive_furnace_kit",
            () -> new PrimitiveFurnaceKitItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ENTANGLED_VEIN_CARD = ITEMS.register("entangled_vein_card",
            () -> new EntangledVeinCardItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ENTANGLED_FLUID_CARD = ITEMS.register("entangled_fluid_card",
            () -> new EntangledFluidCardItem(new Item.Properties().stacksTo(1)));

    public static final java.util.List<RegistryObject<Item>> BOOST_COVERS = createCovers();
    private static java.util.List<RegistryObject<Item>> createCovers() {
        var covers = new java.util.ArrayList<RegistryObject<Item>>();
        for (int tier = 0; tier <= com.gregtechceu.gtceu.api.GTValues.MAX; tier++) {
            final int t = tier;
            String name = com.gregtechceu.gtceu.api.GTValues.VN[tier].toLowerCase(java.util.Locale.ROOT) + "_production_boost_cover";
            covers.add(ITEMS.register(name, () -> {
                var item = com.gregtechceu.gtceu.api.item.ComponentItem.create(new Item.Properties());
                item.attachComponents(new com.gregtechceu.gtceu.common.item.CoverPlaceBehavior(GTOACovers.PRODUCTION_BOOST[t]));
                item.attachComponents(new com.gregtechceu.gtceu.api.item.component.IAddInformation() {
                    @Override public void appendTooltips(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.Level level,
                            java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
                        tooltip.add(Component.translatable("gtoa.cover.tooltip", 1 << (t + 1)));
                    }
                });
                return item;
            }));
        }
        return covers;
    }
    private static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GTOAdditions.MOD_ID);
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("additions", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.gtoa"))
                    .icon(() -> GTOAMachines.PRIMITIVE_STONE_FURNACE.asStack())
                    .displayItems((parameters, output) -> {
                        output.accept(GTOAMachines.PRIMITIVE_STONE_FURNACE.asStack());
                        output.accept(GTOABlocks.UNIVERSAL_FACTORY_CASING.asStack());
                        for (var definition : GTOAMachines.ADDITIONAL_MACHINES)
                            if (definition != GTOAMachines.ENTANGLED_MINER) output.accept(definition.asStack());
                        for (var cover : BOOST_COVERS) output.accept(cover.get());
                        output.accept(PRIMITIVE_FURNACE_KIT.get());
                        output.accept(ENTANGLED_VEIN_CARD.get());
                        output.accept(ENTANGLED_FLUID_CARD.get());
                    })
                    .build());

    private GTOAItems() {}
    public static void register(IEventBus bus) { ITEMS.register(bus); TABS.register(bus); }
}
