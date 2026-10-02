package com.raishxn.gtoa;

import com.raishxn.gtoa.machine.PrimitiveStoneFurnaceMachine;
import com.raishxn.gtoa.machine.ExtendedItemBusMachine;
import com.raishxn.gtoa.machine.ExtendedFluidHatchMachine;
import com.raishxn.gtoa.machine.MagicGeneratorMachine;
import com.raishxn.gtoa.machine.DistillationThermostatHatchMachine;
import com.gtocore.api.machine.part.GTOPartAbility;
import com.gregtechceu.gtceu.common.machine.multiblock.part.FluidHatchPartMachine;
import com.gregtechceu.gtceu.client.renderer.machine.OverlaySteamMachineRenderer;
import com.gregtechceu.gtceu.client.renderer.machine.SimpleGeneratorMachineRenderer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.handler.IO;
import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.client.renderer.machine.OverlayTieredMachineRenderer;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblockpro.Piece;
import com.gregtechceu.gtceu.api.machine.multiblockpro.Structure;
import com.gregtechceu.gtceu.api.machine.multiblockpro.Symbols;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gtolib.api.machine.MultiblockDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;
import static com.gregtechceu.gtceu.api.pattern.util.RelativeDirection.*;

public final class GTOAMachines {
    private GTOAMachines() {}

    public static final MultiblockMachineDefinition PRIMITIVE_STONE_FURNACE =
            GTOAdditions.REGISTRATE.multiblock("primitive_stone_furnace", PrimitiveStoneFurnaceMachine::new)
                    .definition(MultiblockDefinition::createDefinition)
                    .langValue("Primitive Stone Furnace")
                    .nonYAxisRotation()
                    .recipeType(GTRecipeTypes.FURNACE_RECIPES)
                    .appearanceBlock(() -> Blocks.STONE)
                    .structure(definition -> Structure.root(Piece.start(RIGHT, UP, BACK)
                            // BACK advances into the structure: the controller belongs in the first slice.
                            .aisle("AAA", "ASA", "AAA")
                            .aisle("AAA", "A A", "AAA")
                            .aisle("AAA", "AAA", "AAA").build())
                            .symbols(Symbols.create()
                            .where('S', controller(definition))
                            .where('A', blocks(Blocks.STONE)
                                    .or(abilities(PartAbility.IMPORT_ITEMS).setMinGlobalLimited(1))
                                    .or(abilities(PartAbility.EXPORT_ITEMS).setMinGlobalLimited(1)))
                            .where(' ', air()))
                            .build())
                    .workableCasingRenderer(ResourceLocation.fromNamespaceAndPath("minecraft", "block/stone"),
                            GTCEu.id("block/multiblock/primitive_blast_furnace"))
                    .tooltips(Component.translatable("gtoa.furnace.tooltip"))
                    .register();

    public static final List<MachineDefinition> ADDITIONAL_MACHINES = new ArrayList<>();
    public static final MultiblockMachineDefinition UNIVERSAL_FACTORY = universalFactory();
    private static MultiblockMachineDefinition universalFactory() {
        var builder = GTOAdditions.REGISTRATE.multiblock("universal_factory", com.raishxn.gtoa.machine.UniversalFactoryMachine::new)
                .definition(MultiblockDefinition::createDefinition)
                .langValue("Universal Factory").allRotation();
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.BENDER_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.COMPRESSOR_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.FORGE_HAMMER_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.CUTTER_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.EXTRUDER_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.LATHE_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.WIREMILL_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.FORMING_PRESS_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.POLARIZER_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.LASER_ENGRAVER_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.FLUID_SOLIDFICATION_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.ASSEMBLER_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.ARC_FURNACE_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.CIRCUIT_ASSEMBLER_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.CANNER_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.CENTRIFUGE_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.THERMAL_CENTRIFUGE_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.ELECTROLYZER_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.SIFTER_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.MACERATOR_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.EXTRACTOR_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.CHEMICAL_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.MIXER_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.CHEMICAL_BATH_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.ORE_WASHER_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.LARGE_CHEMICAL_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.PACKER_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.DISTILLERY_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.AUTOCLAVE_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.FLUID_HEATER_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.BREWING_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.FERMENTING_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.LAMINATOR_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.LOOM_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.LASER_WELDER_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.CLUSTER_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.ROLLING_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.DEHYDRATOR_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.UNPACKER_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.ELECTROMAGNETIC_SEPARATOR_RECIPES);
        builder.recipeType(com.gtocore.common.data.GTORecipeTypes.ALLOY_SMELTER_RECIPES);
        var definition = builder
                .appearanceBlock(GTOABlocks.UNIVERSAL_FACTORY_CASING)
                .structure(d -> Structure.root(Piece.start(RIGHT, UP, BACK)
                        .aisle("AAA", "ASA", "AAA")
                        .aisle("AAA", "ABA", "AAA")
                        .aisle("AAA", "AAA", "AAA").build())
                        .symbols(Symbols.create()
                                .where('S', controller(d))
                                .where('A', blocks(GTOABlocks.UNIVERSAL_FACTORY_CASING.get())
                                        .or(abilities(PartAbility.IMPORT_ITEMS).setPreviewCount(1))
                                        .or(abilities(PartAbility.EXPORT_ITEMS).setPreviewCount(1))
                                        .or(abilities(PartAbility.IMPORT_FLUIDS).setPreviewCount(1))
                                        .or(abilities(PartAbility.EXPORT_FLUIDS).setPreviewCount(1))
                                        .or(abilities(PartAbility.INPUT_ENERGY).setPreviewCount(1))
                                        .or(abilities(PartAbility.MAINTENANCE).setMinGlobalLimited(1).setMaxGlobalLimited(1)))
                                .where('B', frames(com.gregtechceu.gtceu.common.data.GTMaterials.Steel)))
                        .build())
                .workableCasingRenderer(ResourceLocation.fromNamespaceAndPath("gtoa", "block/casings/universal_factory_casing"),
                        GTCEu.id("block/multiblock/assembly_line"))
                .tooltips(Component.translatable("gtoa.factory.tooltip"), Component.translatable("gtoa.factory.structure"))
                .register();
        track(definition);
        return definition;
    }


    public static final MachineDefinition[] EXTENDED_INPUT_BUSES = buses(IO.IN);
    public static final MachineDefinition[] EXTENDED_OUTPUT_BUSES = buses(IO.OUT);
    public static final MachineDefinition ULV_EXTENDED_INPUT_BUS = EXTENDED_INPUT_BUSES[GTValues.ULV];
    public static final MachineDefinition ULV_EXTENDED_OUTPUT_BUS = EXTENDED_OUTPUT_BUSES[GTValues.ULV];
    public static final MachineDefinition[][] EXTENDED_INPUT_HATCHES = hatches(IO.IN);
    public static final MachineDefinition[][] EXTENDED_OUTPUT_HATCHES = hatches(IO.OUT);
    public static final MachineDefinition STEAM_EXTENDED_OUTPUT_BUS = bus("steam_extended_output_bus", GTValues.LV, IO.OUT, true);
    public static final MachineDefinition STEAM_EXTENDED_OUTPUT_HATCH = hatch("steam_extended_output_hatch", GTValues.LV, IO.OUT, 1, true);
    public static final MachineDefinition[] MAGIC_GENERATORS = magicGenerators();
    public static final MachineDefinition ULV_MAGIC_GENERATOR = MAGIC_GENERATORS[GTValues.ULV];
    public static final MachineDefinition LV_MAGIC_GENERATOR = MAGIC_GENERATORS[GTValues.LV];
    public static final MachineDefinition DISTILLATION_THERMOSTAT_HATCH = track(
            GTOAdditions.REGISTRATE.machine("distillation_thermostat_hatch", DistillationThermostatHatchMachine::new)
                    .tier(GTValues.ULV).langValue("Primitive Distillation Thermostat Hatch")
                    .rotationState(com.gregtechceu.gtceu.api.data.RotationState.ALL)
                    .renderer(() -> new OverlayTieredMachineRenderer(GTValues.ULV, ResourceLocation.fromNamespaceAndPath(GTOAdditions.MOD_ID, "block/machine/part/distillation_thermostat_hatch")))
                    .tooltips(Component.translatable("gtoa.distillation.tooltip")).register());

    public static final MachineDefinition[] ENTANGLED_MINERS = entangledMiners();
    // Preserve the old controller ID in existing worlds; this is now a singleblock MV machine.
    public static final MachineDefinition ENTANGLED_MINER = entangledMiner("entangled_miner", GTValues.MV, false);

    private static MachineDefinition[] entangledMiners() {
        var result = new MachineDefinition[5];
        result[0] = entangledMiner("steam_entangled_miner", GTValues.ULV, true);
        for (int tier = GTValues.LV; tier <= GTValues.EV; tier++)
            result[tier] = entangledMiner(tierName(tier) + "_entangled_miner", tier, false);
        return result;
    }
    private static MachineDefinition entangledMiner(String name, int tier, boolean steam) {
        var type = steam ? GTOARecipeTypes.STEAM_ENTANGLED_MINER : GTOARecipeTypes.ENTANGLED_MINER;
        var builder = GTOAdditions.REGISTRATE.machine(name,
                        holder -> new com.raishxn.gtoa.machine.EntangledMinerMachine(holder, tier, steam))
                .tier(tier).langValue((steam ? "Steam" : GTValues.VN[tier]) + " Entangled Miner")
                .nonYAxisRotation().recipeType(type)
                .editableUI(steam ? com.raishxn.gtoa.machine.EntangledMachineUI.steam(type)
                        : com.gregtechceu.gtceu.api.machine.SimpleTieredMachine.EDITABLE_UI_CREATOR.apply(
                                ResourceLocation.fromNamespaceAndPath("gtoa", "entangled_miner"), type))
                .tooltips(Component.translatable("gtoa.entangled.singleblock.tooltip",
                        com.raishxn.gtoa.machine.EntangledRates.rawPerSecond(steam ? 0 : tier)))
                .tooltips(Component.translatable(steam ? "gtoa.entangled.steam.tooltip" : "gtoa.entangled.electric.tooltip",
                        steam ? 80 : GTValues.V[tier]))
                .allowCoverOnFront(true);
        if (steam) builder.workableSteamHullRenderer(false, ResourceLocation.fromNamespaceAndPath(GTOAdditions.MOD_ID, "block/machines/steam_entangled_miner"));
        else builder.workableTieredHullRenderer(ResourceLocation.fromNamespaceAndPath(GTOAdditions.MOD_ID, "block/machines/entangled_miner"));
        return track(builder.register());
    }

    public static final MachineDefinition[] ENTANGLED_OIL_DRILLS = entangledOilDrills();
    private static MachineDefinition[] entangledOilDrills() {
        var result = new MachineDefinition[5];
        for (int level = 0; level <= GTValues.EV; level++) {
            final int tier = level;
            boolean steam = tier == 0;
            String name = (steam ? "steam" : tierName(tier)) + "_entangled_oil_drill";
            var type = steam ? GTOARecipeTypes.STEAM_ENTANGLED_OIL_DRILL : GTOARecipeTypes.ENTANGLED_OIL_DRILL;
            var builder = GTOAdditions.REGISTRATE.machine(name,
                            holder -> new com.raishxn.gtoa.machine.EntangledOilDrillMachine(holder, tier, steam))
                    .tier(tier).langValue((steam ? "Steam" : GTValues.VN[tier]) + " Entangled Oil Drill")
                    .nonYAxisRotation().recipeType(type)
                    .editableUI(steam ? com.raishxn.gtoa.machine.EntangledMachineUI.steam(type)
                            : com.gregtechceu.gtceu.api.machine.SimpleTieredMachine.EDITABLE_UI_CREATOR.apply(
                                    ResourceLocation.fromNamespaceAndPath("gtoa", "entangled_oil_drill"), type))
                    .tooltips(Component.translatable("gtoa.oil.tooltip", 1 << tier, 64000 << tier))
                    .tooltips(Component.translatable(steam ? "gtoa.entangled.steam.tooltip" : "gtoa.entangled.electric.tooltip",
                            steam ? 80 : GTValues.V[tier]))
                    .allowCoverOnFront(true);
            if (steam) builder.workableSteamHullRenderer(false, ResourceLocation.fromNamespaceAndPath(GTOAdditions.MOD_ID, "block/machines/steam_entangled_oil_drill"));
            else builder.workableTieredHullRenderer(ResourceLocation.fromNamespaceAndPath(GTOAdditions.MOD_ID, "block/machines/entangled_oil_drill"));
            result[tier] = track(builder.register());
        }
        return result;
    }

    private static MachineDefinition track(MachineDefinition definition) { ADDITIONAL_MACHINES.add(definition); return definition; }
    private static String tierName(int tier) { return GTValues.VN[tier].toLowerCase(Locale.ROOT); }
    private static MachineDefinition[] buses(IO io) {
        var result = new MachineDefinition[GTValues.MAX + 1];
        for (int tier = 0; tier <= GTValues.MAX; tier++)
            result[tier] = bus(tierName(tier) + "_extended_" + (io == IO.IN ? "input" : "output") + "_bus", tier, io, false);
        return result;
    }
    private static MachineDefinition bus(String name, int tier, IO io, boolean steam) {
        String model = "block/machine/part/" + (io == IO.IN ? "item_bus.import" : "item_bus.export");
        return track(GTOAdditions.REGISTRATE.machine(name, holder -> new ExtendedItemBusMachine(holder, tier, io, steam))
                .tier(tier).langValue((steam ? "Steam" : GTValues.VN[tier]) + " Extended " + (io == IO.IN ? "Input" : "Output") + " Bus")
                .rotationState(com.gregtechceu.gtceu.api.data.RotationState.ALL)
                .abilities(steam ? PartAbility.STEAM_EXPORT_ITEMS : io == IO.IN ? PartAbility.IMPORT_ITEMS : PartAbility.EXPORT_ITEMS)
                .renderer(() -> steam ? new OverlaySteamMachineRenderer(GTCEu.id(model)) : new OverlayTieredMachineRenderer(tier, GTCEu.id(model)))
                .tooltips(Component.translatable("gtoa.extended_bus.tooltip"))
                .tooltips(Component.translatable(io == IO.OUT ? "gtoa.boost.tooltip" : "gtoa.input.tooltip", steam ? 8 : 1 << (tier + 1)))
                .allowCoverOnFront(true).register());
    }
    private static MachineDefinition[][] hatches(IO io) {
        var result = new MachineDefinition[3][GTValues.MAX + 1];
        int[] slots = {1, 4, 9};
        for (int variant = 0; variant < slots.length; variant++) {
            for (int tier = 0; tier <= GTValues.MAX; tier++) {
                String name = tierName(tier) + "_extended_" + (io == IO.IN ? "input" : "output") + "_hatch" + (slots[variant] == 1 ? "" : "_" + slots[variant] + "x");
                result[variant][tier] = hatch(name, tier, io, slots[variant], false);
            }
        }
        return result;
    }
    private static MachineDefinition hatch(String name, int tier, IO io, int slots, boolean steam) {
        int initial = slots == 1 ? FluidHatchPartMachine.INITIAL_TANK_CAPACITY_1X : slots == 4 ? FluidHatchPartMachine.INITIAL_TANK_CAPACITY_4X : FluidHatchPartMachine.INITIAL_TANK_CAPACITY_9X;
        PartAbility specific = io == IO.IN ? (slots == 1 ? PartAbility.IMPORT_FLUIDS_1X : slots == 4 ? PartAbility.IMPORT_FLUIDS_4X : PartAbility.IMPORT_FLUIDS_9X)
                : (slots == 1 ? PartAbility.EXPORT_FLUIDS_1X : slots == 4 ? PartAbility.EXPORT_FLUIDS_4X : PartAbility.EXPORT_FLUIDS_9X);
        String model = "block/machine/part/fluid_hatch." + (io == IO.IN ? "import" : "export") + (slots == 1 ? "" : "_" + slots + "x");
        return track(GTOAdditions.REGISTRATE.machine(name, holder -> new ExtendedFluidHatchMachine(holder, tier, io, initial, slots, steam))
                .tier(tier).langValue((steam ? "Steam" : GTValues.VN[tier]) + " Extended " + (io == IO.IN ? "Input" : "Output") + " Hatch" + (slots == 1 ? "" : " (" + slots + " tanks)"))
                .rotationState(com.gregtechceu.gtceu.api.data.RotationState.ALL)
                .abilities(steam ? GTOPartAbility.STEAM_EXPORT_FLUIDS : io == IO.IN ? PartAbility.IMPORT_FLUIDS : PartAbility.EXPORT_FLUIDS, specific)
                .renderer(() -> steam ? new OverlaySteamMachineRenderer(GTCEu.id(model)) : new OverlayTieredMachineRenderer(tier, GTCEu.id(model)))
                .tooltips(Component.translatable("gtoa.extended_hatch.tooltip"))
                .tooltips(Component.translatable(io == IO.OUT ? "gtoa.boost.tooltip" : "gtoa.input.tooltip", steam ? 8 : 1 << (tier + 1)))
                .allowCoverOnFront(true).register());
    }
    private static MachineDefinition[] magicGenerators() {
        var result = new MachineDefinition[GTValues.MAX + 1];
        for (int tier = 0; tier <= GTValues.MAX; tier++)
            result[tier] = magicGenerator(tier);
        return result;
    }
    private static MachineDefinition magicGenerator(int tier) {
        long gen = GTValues.V[tier] * 256L;
        long voltage = GTValues.V[tier];
        long capacity = voltage * 512L;
        return track(GTOAdditions.REGISTRATE.machine(tierName(tier) + "_magic_generator", holder -> new MagicGeneratorMachine(holder, tier))
                .tier(tier).langValue(GTValues.VN[tier] + " Primitive Magic Generator")
                .rotationState(com.gregtechceu.gtceu.api.data.RotationState.NON_Y_AXIS)
                .renderer(() -> new SimpleGeneratorMachineRenderer(tier, GTCEu.id("block/generators/gas_turbine")))
                .tooltips(Component.translatable("gtoa.magic.tooltip", gen, voltage, capacity))
                .register());
    }

    // Only the early coremod calls this method. Accessing the class initializes all definitions.
    public static void init() {
        MachineRegistrationState.complete();
        com.mojang.logging.LogUtils.getLogger().info("GTO-Additions machines registered in GTOMachines initialization");
    }

    public static void validateLoaded() {
        var definitions = new ArrayList<>(ADDITIONAL_MACHINES);
        definitions.add(PRIMITIVE_STONE_FURNACE);
        for (MachineDefinition definition : definitions) {
            if (com.gregtechceu.gtceu.api.registry.GTRegistries.MACHINES.get(definition.getId()) != definition
                    || net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(definition.getId()) != definition.asItem()
                    || net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(definition.getId()) != definition.get()) {
                throw new IllegalStateException("GTO-Additions machine missing from loaded registries: " + definition.getId());
            }
        }
    }
}
