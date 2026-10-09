package com.raishxn.gtoa.godforge;

import com.raishxn.gtoa.GTOAdditions;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Graviton Shard (a gem material in GTNA, a plain item here because GTO has no such material) and the GTNH crafting
 * components of the Forge of Gods recipes that GTO lacks, with the GTNA textures.
 */
public final class GodforgeItems {

    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GTOAdditions.MOD_ID);
    public static final List<RegistryObject<Item>> ALL = new ArrayList<>();

    public static final RegistryObject<Item> GRAVITON_SHARD = item("graviton_shard");
    public static final RegistryObject<Item> STABLE_BOSON_CONTAINMENT_UNIT = item("stable_boson_containment_unit");
    public static final RegistryObject<Item> SUPERCONDUCTOR_COMPOSITE = item("superconductor_composite");
    public static final RegistryObject<Item> ELECTROMAGNET_TENGAM = item("electromagnet_tengam");
    public static final RegistryObject<Item> TESSERACT = item("tesseract");
    public static final RegistryObject<Item> THERMAL_SUPERCONDUCTOR = item("thermal_superconductor");
    public static final RegistryObject<Item> RELATIVISTIC_HEAT_CAPACITOR = item("relativistic_heat_capacitor");
    public static final RegistryObject<Item> NEUTRONIUM_HEAT_CAPACITOR = item("neutronium_heat_capacitor");
    public static final RegistryObject<Item> SPACE_COOLANT_CELL = item("space_coolant_cell");
    public static final RegistryObject<Item> GRAVITON_ANOMALY = item("graviton_anomaly");
    public static final RegistryObject<Item> REALLY_ULTIMATE_BATTERY = item("really_ultimate_battery");
    public static final RegistryObject<Item> EXTREMELY_ULTIMATE_BATTERY = item("extremely_ultimate_battery");
    public static final RegistryObject<Item> INSANELY_ULTIMATE_BATTERY = item("insanely_ultimate_battery");
    public static final RegistryObject<Item> MEGA_ULTIMATE_BATTERY = item("mega_ultimate_battery");

    private GodforgeItems() {}

    private static RegistryObject<Item> item(String id) {
        var entry = ITEMS.register(id, () -> new Item(new Item.Properties()));
        ALL.add(entry);
        return entry;
    }

    public static ItemStack gravitonShard(int count) {
        return new ItemStack(GRAVITON_SHARD.get(), count);
    }

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
