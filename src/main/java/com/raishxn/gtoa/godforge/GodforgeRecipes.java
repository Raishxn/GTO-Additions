package com.raishxn.gtoa.godforge;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.PropertyKey;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipeDefinition;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTItems;
import com.gregtechceu.gtceu.common.data.GTMachines;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.common.data.GTRecipeDataKeys;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.machines.GTMultiMachines;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.gtocore.api.data.tag.GTOTagPrefix;
import com.gtocore.common.data.GTOBlocks;
import com.gtocore.common.data.GTOItems;
import com.gtocore.common.data.GTOMaterials;
import com.gtocore.common.data.machines.GCYMMachines;
import com.gtocore.common.data.machines.MultiBlockD;
import com.gtolib.api.recipe.RecipeBuilder;
import com.gregtechceu.gtceu.api.recipe.GTRecipeBuilder;
import com.raishxn.gtoa.GTOAdditions;
import com.raishxn.gtoa.godforge.api.GodforgePlasmaTable;
import com.raishxn.gtoa.godforge.api.GodforgeUpgrade;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Forge of Gods recipes, ported from GTNA ({@code GTNAGodforgeRecipes}, {@code GTNAGodforgeProgression} and the molten
 * copy of {@code GTNARecipeType}). Called from GTO's {@code Data.commonInit} by the {@code gtoa_godforge_recipes}
 * coremod, after GTO's own recipes and before {@code RecipeBuilder.finish()}.
 *
 * <p>
 * Shapes, amounts, durations and tiers follow GTNA/GTNH. GTNH materials and items that GTO lacks are replaced by GTO
 * ones of the same tier; the table is in {@code docs/FORGE_OF_GODS.md}.
 */
public final class GodforgeRecipes {

    private static final int INGOTS = 144;
    private static final int STACKS = 64 * INGOTS;
    private static final int SECONDS = 20;
    /**
     * Research Station data. GTNH asks 48M CWU; GTO's own top machines ask far more, so the Forge line is scaled to
     * GTO: parts 8192 CWU/t for 256M CWU, module controllers 16384 CWU/t for 1G CWU and the Forge itself
     * 32768 CWU/t for 2G CWU, all at OpV research power.
     */
    private static final int CWUT = 8192;
    private static final int TOTAL_CWU = 256_000_000;
    private static final int MODULE_CWUT = 16384;
    private static final int MODULE_TOTAL_CWU = 1_000_000_000;
    private static final int FORGE_CWUT = 32768;
    private static final int FORGE_TOTAL_CWU = 2_000_000_000;

    private GodforgeRecipes() {}

    /**
     * GTCEu's base builder: GTO's native {@code RecipeBuilder.save()} silently drops recipes saved by addons (it
     * returns the default definition), while the base builder registers into the recipe type directly.
     */
    private static GTRecipeBuilder gt(com.gregtechceu.gtceu.api.recipe.GTRecipeType type, net.minecraft.resources.ResourceLocation id) {
        return new GTRecipeBuilder(id, type);
    }

    public static void init() {
        run("plasma", GodforgeRecipes::plasma);
        run("progression", GodforgeRecipes::progression);
        run("extra costs", GodforgeRecipes::registerExtraCosts);
    }

    /** Runs right after GTO's {@code RecipeBuilder.finish()}: reports how many Forge recipes are registered. */
    public static void verify() {
        long ours = com.gregtechceu.gtceu.api.registry.GTRegistries.RECIPE_TYPES.values().stream()
                .flatMap(t -> t.recipes.keySet().stream()).filter(id -> id.getNamespace().equals(GTOAdditions.MOD_ID))
                .count();
        com.mojang.logging.LogUtils.getLogger().info("GTO-Additions Forge of Gods: {} GT recipes registered", ours);
    }

    /**
     * Research Station data for a recipe. GTCEu's {@code StationBuilder} saves its station recipe through GTO's
     * native builder, which drops addon recipes, so the station recipe is built here with the base builder.
     */
    private static com.gregtechceu.gtceu.common.recipe.condition.ResearchCondition research(ItemStack researchStack,
                                                                                           int cwut, int totalCwu,
                                                                                           long eut) {
        String researchId = "gtoa_godforge_" + BuiltInRegistries.ITEM.getKey(researchStack.getItem()).getPath();
        Item dataItem = com.gregtechceu.gtceu.utils.ResearchManager.DATA_ITEM_PROVIDER.apply(cwut);
        ItemStack dataStack = dataItem.getDefaultInstance();
        com.gregtechceu.gtceu.utils.ResearchManager.writeResearchToNBT(dataStack.getOrCreateTag(), researchId,
                GTRecipeTypes.ASSEMBLY_LINE_RECIPES);
        if (RESEARCHED.add(researchId)) {
            new GTRecipeBuilder(GTOAdditions.id("research/" + researchId), GTRecipeTypes.RESEARCH_STATION_RECIPES)
                    .inputItems(new ItemStack(dataItem)).inputItems(researchStack.copyWithCount(1))
                    .outputItems(dataStack.copy()).EUt(eut).CWUt(cwut).totalCWU(totalCwu).save();
        }
        return new com.gregtechceu.gtceu.common.recipe.condition.ResearchCondition(researchId, dataStack);
    }

    private static final java.util.Set<String> RESEARCHED = new java.util.HashSet<>();

    /** A failure here must never abort GTO's own recipe loading (it runs inside {@code Data.commonInit}). */
    private static void run(String stage, Runnable action) {
        try {
            action.run();
            com.mojang.logging.LogUtils.getLogger().info("GTO-Additions Forge of Gods recipes: {} done", stage);
        } catch (Throwable t) {
            com.mojang.logging.LogUtils.getLogger().error("GTO-Additions Forge of Gods recipes: {} failed", stage, t);
        }
    }

    // ------------------------------------------------------------------ GTNH -> GTO substitutions

    /** GTNH Creon (UMV metal) -> GTO Cosmic Neutronium. */
    private static Material creon() {
        return GTOMaterials.CosmicNeutronium;
    }

    /** GTNH Mellion (UMV metal) -> GTO Shirabon. */
    private static Material mellion() {
        return GTOMaterials.Shirabon;
    }

    /** GTNH Six-Phased Copper -> GTO Awakened Draconium. */
    private static Material sixPhasedCopper() {
        return GTOMaterials.AwakenedDraconium;
    }

    /** GTNH UIV superconductor -> GTO's UXV cable material (Taranium). */
    private static Material superconductorUIV() {
        return GTOMaterials.Taranium;
    }

    /** GTNH UMV superconductor base -> GTO's OpV cable material (Crystal Matrix). */
    private static Material superconductorUMV() {
        return GTOMaterials.CrystalMatrix;
    }

    /** GTNH Metastable Oganesson: GTO's only exists as a fluid -> Heavy Quark Degenerate Matter. */
    private static Material metastableOganesson() {
        return GTOMaterials.HeavyQuarkDegenerateMatter;
    }

    /** GTNH Rhugnor gears: GTO's Rhugnor is fluid-only -> Celestial Tungsten gears. */
    private static Material rhugnorSolid() {
        return GTOMaterials.CelestialTungsten;
    }

    /** GTNH Ichorium -> GTO Starmetal. */
    private static Material ichorium() {
        return GTOMaterials.Starmetal;
    }

    /** GTNH Dragon Metal -> GTO Draconium. */
    private static Material dragonMetal() {
        return GTOMaterials.Draconium;
    }

    /** GTNH Universium -> GTO Chaos. */
    private static Material universium() {
        return GTOMaterials.Chaos;
    }

    /** GTNH Phonon Medium -> GTO Dimensionally Shifted Superfluid. */
    private static FluidStack phonon(int amount) {
        return fluid(GTOMaterials.DimensionAllyshiftedSuperFluid, amount);
    }

    // ------------------------------------------------------------------ helpers

    /** A material form, falling back to coarser forms the material does generate. */
    private static ItemStack form(TagPrefix prefix, Material material, int amount) {
        TagPrefix[] chain = prefix == TagPrefix.plateDense ? new TagPrefix[] { TagPrefix.plateDense, TagPrefix.plateDouble,
                TagPrefix.plate, TagPrefix.ingot } : prefix == TagPrefix.gearSmall ?
                        new TagPrefix[] { TagPrefix.gearSmall, TagPrefix.gear, TagPrefix.plate } :
                        prefix == TagPrefix.gear ? new TagPrefix[] { TagPrefix.gear, TagPrefix.plateDouble, TagPrefix.plate, TagPrefix.ingot } :
                        prefix == TagPrefix.rodLong ? new TagPrefix[] { TagPrefix.rodLong, TagPrefix.rod, TagPrefix.ingot } :
                                prefix == TagPrefix.lens ? new TagPrefix[] { TagPrefix.lens, TagPrefix.gemExquisite,
                                        TagPrefix.plate } :
                                        new TagPrefix[] { prefix, TagPrefix.plate, TagPrefix.ingot, TagPrefix.dust };
        for (TagPrefix candidate : chain) {
            ItemStack stack = ChemicalHelper.get(candidate, material, Math.min(64, amount));
            if (!stack.isEmpty()) return stack;
        }
        com.mojang.logging.LogUtils.getLogger().error("GTO-Additions Forge of Gods recipe: no {} for {}", prefix, material);
        return ItemStack.EMPTY;
    }

    private static ItemStack item(ItemLike item, int amount) {
        return new ItemStack(item, amount);
    }

    private static ItemStack machine(MachineDefinition[] tiers, int tier, int amount) {
        for (int t = tier; t >= 0; t--) {
            if (t < tiers.length && tiers[t] != null) return tiers[t].asStack(amount);
        }
        return ItemStack.EMPTY;
    }

    private static FluidStack fluid(Material material, int amount) {
        Fluid fluid = material.getFluid();
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }

    private static FluidStack molten(Material material, int amount) {
        Fluid fluid = material.getFluid(FluidStorageKeys.MOLTEN);
        if (fluid == null) fluid = material.getFluid();
        return fluid == null ? FluidStack.EMPTY : new FluidStack(fluid, amount);
    }

    private static FluidStack plasma(Material material, int amount) {
        Fluid fluid = material.getFluid(FluidStorageKeys.PLASMA);
        return fluid == null ? molten(material, amount) : new FluidStack(fluid, amount);
    }

    /** GTNH Mutated Living Solder. */
    private static FluidStack solder(int amount) {
        return fluid(GTOMaterials.MutatedLivingSolder, amount);
    }

    /** GTNH ZPM4 (Extremely Ultimate Battery) -> GTO UXV battery. */
    private static ItemStack battery(int amount) {
        return item(GTOItems.EXTREMELY_MAX_BATTERY.get(), amount);
    }

    /** GTNH ZPM6 (Mega Ultimate Battery) -> GTO OpV battery. */
    private static ItemStack megaBattery(int amount) {
        return item(GTOItems.INSANELY_MAX_BATTERY.get(), amount);
    }

    /** Eternal Singularity -> Gravi Stars (as GTNA). */
    private static ItemStack singularity(int amount) {
        return item(GTItems.GRAVI_STAR.get(), amount);
    }

    /** TecTech UIV energy tunnel -> GTCEu UIV 4096A laser target hatch. */
    private static ItemStack tunnel() {
        return machine(GTMachines.LASER_INPUT_HATCH_4096, GTValues.UIV, 1);
    }

    /** BartWorks UMV energy distributor -> GTCEu UXV power transformer. */
    private static ItemStack distributor() {
        return machine(GTMachines.POWER_TRANSFORMER, GTValues.UXV, 1);
    }

    /** GT++ Fusion MK4 / Compact Fusion MK5 -> GTO Kuangbiao One giant fusion reactor (GTNA's Advanced Fusion). */
    private static ItemStack advancedFusion(int amount) {
        return MultiBlockD.KUANGBIAO_ONE_GIANT_NUCLEAR_FUSION_REACTOR.asStack(amount);
    }

    /** GTNH Transcendent Plasma Mixer -> GTO Hyperdimensional Plasma Fusion Core. */
    private static ItemStack plasmaMixer(int amount) {
        return MultiBlockD.HYPERDIMENSIONAL_PLASMA_FUSION_CORE.asStack(amount);
    }

    private static ItemStack component(net.minecraftforge.registries.RegistryObject<Item> entry, int amount) {
        return new ItemStack(entry.get(), amount);
    }

    private static void assemblyLine(String id, ItemStack research, List<ItemStack> items, TagKey<Item> circuits,
                                     int circuitCount, List<FluidStack> fluids, ItemStack output, int duration, long eut) {
        GTRecipeBuilder builder = gt(com.gtocore.common.data.GTORecipeTypes.ASSEMBLY_LINE_RECIPES, GTOAdditions.id("godforge/" + id));
        for (ItemStack stack : items) if (!stack.isEmpty()) builder.inputItems(stack);
        if (circuits != null) builder.inputItems(circuits, circuitCount);
        for (FluidStack stack : fluids) if (!stack.isEmpty()) builder.inputFluids(stack);
        boolean forge = output.is(GodforgeMachines.FORGE_OF_GODS.asItem());
        boolean module = !forge && BuiltInRegistries.ITEM.getKey(output.getItem()).getPath().startsWith("godforge_");
        int cwut = forge ? FORGE_CWUT : module ? MODULE_CWUT : CWUT;
        int total = forge ? FORGE_TOTAL_CWU : module ? MODULE_TOTAL_CWU : TOTAL_CWU;
        long researchEUt = GTValues.VA[forge || module ? GTValues.OpV : GTValues.UXV];
        builder.outputItems(output).duration(duration).EUt(eut)
                .addCondition(research(research, cwut, total, researchEUt))
                .save();
    }

    // ------------------------------------------------------------------ plasma module

    /** GTNH {@code Godforge.run()} plasma table at the GTNH UXV recipe tier (GTNA {@code GTNAGodforgeRecipes}). */
    private static void plasma() {
        long eut = GTValues.VA[GTValues.UXV];
        for (var entry : GodforgePlasmaTable.ENTRIES) {
            Material material = GodforgeMaterialRefs.material(entry.material());
            if (material == null || !material.hasProperty(PropertyKey.FLUID)) continue;
            Fluid plasma = material.getFluid(FluidStorageKeys.PLASMA);
            if (plasma == null) continue;
            if (entry.solid()) {
                if (material.hasProperty(PropertyKey.DUST)) {
                    gt(GodforgeRecipeTypes.GODFORGE_PLASMA_RECIPES, GTOAdditions.id("godforge_plasma_" +
                            entry.material() + "_dust"))
                            .inputItems(TagPrefix.dust, material)
                            .outputFluids(new FluidStack(plasma, 144))
                            .duration(entry.durationTicks()).EUt(eut)
                            .addData(GodforgeRecipeTypes.PLASMA_TIER, entry.tier())
                            .addData(GodforgeRecipeTypes.PLASMA_MULTISTEP, entry.multiStep())
                            .save();
                }
                Fluid molten = material.getFluid(FluidStorageKeys.MOLTEN);
                if (molten == null) molten = material.getFluid(FluidStorageKeys.LIQUID);
                if (molten != null) {
                    gt(GodforgeRecipeTypes.GODFORGE_PLASMA_RECIPES, GTOAdditions.id("godforge_plasma_" +
                            entry.material() + "_molten"))
                            .inputFluids(new FluidStack(molten, 144))
                            .outputFluids(new FluidStack(plasma, 144))
                            .duration(entry.durationTicks()).EUt(eut)
                            .addData(GodforgeRecipeTypes.PLASMA_TIER, entry.tier())
                            .addData(GodforgeRecipeTypes.PLASMA_MULTISTEP, entry.multiStep())
                            .save();
                }
            } else {
                Fluid source = material.getFluid();
                if (source == null || source == plasma) continue;
                gt(GodforgeRecipeTypes.GODFORGE_PLASMA_RECIPES, GTOAdditions.id("godforge_plasma_" +
                        entry.material()))
                        .inputFluids(new FluidStack(source, 500))
                        .outputFluids(new FluidStack(plasma, 500))
                        .duration(entry.durationTicks()).EUt(eut)
                        .addData(GodforgeRecipeTypes.PLASMA_TIER, entry.tier())
                        .addData(GodforgeRecipeTypes.PLASMA_MULTISTEP, entry.multiStep())
                        .save();
            }
        }
    }

    // ------------------------------------------------------------------ progression

    /** GTNH {@code addGodforgeRecipes} (GTNA {@code GTNAGodforgeProgression.register}) with GTO substitutes. */
    private static void progression() {
        long umv = GTValues.VA[GTValues.UXV];
        long uiv = GTValues.VA[GTValues.UIV];
        var siphon = GodforgeBlocks.STELLAR_ENERGY_SIPHON_CASING.get();
        var confinement = GodforgeBlocks.MAGNETIC_CONFINEMENT_CASING.get();
        var boundless = GodforgeBlocks.BOUNDLESS_STRUCTURE_CASING.get();
        var guidance = GodforgeBlocks.GUIDANCE_CASING.get();
        var shielding = GodforgeBlocks.SINGULARITY_SHIELDING_CASING.get();
        var conduit = GodforgeBlocks.HARMONIC_PHONON_TRANSMISSION_CONDUIT.get();
        var hypogenCoil = GodforgeBlocks.HYPOGEN_COIL.get();
        var boson = GodforgeItems.STABLE_BOSON_CONTAINMENT_UNIT;
        var relativistic = GodforgeItems.RELATIVISTIC_HEAT_CAPACITOR;
        var dtec = GTOMaterials.ExcitedDtec;
        var transcendent = GTOMaterials.TranscendentMetal;

        // Controller
        assemblyLine("forge_of_gods", item(siphon, 1),
                List.of(item(siphon, 4), battery(2), item(GTOBlocks.DIMENSIONAL_BRIDGE_CASING.get(), 64),
                        singularity(32), form(TagPrefix.plateDense, mellion(), 16),
                        form(TagPrefix.plateDense, sixPhasedCopper(), 16), form(TagPrefix.plateDense, creon(), 16),
                        form(TagPrefix.plateDense, metastableOganesson(), 16), component(boson, 8),
                        form(TagPrefix.wireGtHex, superconductorUIV(), 16), item(GTItems.SENSOR_UIV.get(), 32),
                        tunnel(), distributor()),
                CustomTags.UIV_CIRCUITS, 64,
                List.of(solder(32 * STACKS), fluid(dtec, 8_192_000), plasma(GTMaterials.Thorium, 4 * STACKS),
                        molten(transcendent, 32 * STACKS)),
                GodforgeMachines.FORGE_OF_GODS.asStack(), 300 * SECONDS, umv);

        // Magnetic Confinement Casing (GTO also has its own cheaper assembler recipe for the same block)
        assemblyLine("magnetic_confinement_casing", form(TagPrefix.frameGt, transcendent, 1),
                List.of(form(TagPrefix.frameGt, transcendent, 8),
                        form(TagPrefix.block, GTOMaterials.MagnetoResonatic, 16),
                        form(TagPrefix.plateDense, GTOMaterials.AttunedTengam, 32),
                        form(TagPrefix.plate, creon(), 16), form(TagPrefix.screw, GTOMaterials.Hypogen, 8),
                        form(TagPrefix.screw, sixPhasedCopper(), 8),
                        component(GodforgeItems.SUPERCONDUCTOR_COMPOSITE, 1), item(GTItems.EMITTER_UIV.get(), 2),
                        component(GodforgeItems.ELECTROMAGNET_TENGAM, 1)),
                null, 0, List.of(solder(16 * INGOTS), plasma(GTMaterials.Plutonium241, 16 * INGOTS)),
                item(confinement, 8), 50 * SECONDS, uiv);

        // Boundless Gravitationally Severed Structure Casing
        assemblyLine("boundless_structure_casing", item(confinement, 1),
                List.of(form(TagPrefix.frameGt, mellion(), 16), form(TagPrefix.frameGt, sixPhasedCopper(), 16),
                        form(TagPrefix.frameGt, transcendent, 8),
                        form(TagPrefix.frameGt, GTOMaterials.AstralTitanium, 8),
                        form(TagPrefix.plate, creon(), 6), component(boson, 1), item(GTItems.FIELD_GENERATOR_UEV.get(), 2),
                        item(GTItems.GRAVI_STAR.get(), 4)),
                null, 0, List.of(solder(16 * INGOTS), plasma(GTMaterials.Lead, 2 * INGOTS)),
                item(boundless, 1), 10 * SECONDS, uiv);

        // Celestial Matter Guidance Casing
        assemblyLine("guidance_casing", item(boundless, 1),
                List.of(item(boundless, 1), item(GTOItems.TRANSCENDENT_MAX_BATTERY.get(), 1),
                        item(GTOItems.COSMIC_FABRIC.get(), 1), item(GTItems.FIELD_GENERATOR_UEV.get(), 2),
                        item(GTItems.EMITTER_UIV.get(), 3), form(TagPrefix.plate, creon(), 6),
                        form(TagPrefix.gear, creon(), 8), form(TagPrefix.gearSmall, mellion(), 8)),
                null, 0, List.of(solder(16 * INGOTS), plasma(GTMaterials.Thorium, 2 * INGOTS)),
                item(guidance, 1), 10 * SECONDS, uiv);

        // Stellar Energy Siphon Casing
        assemblyLine("stellar_energy_siphon_casing", item(guidance, 1),
                List.of(item(boundless, 1), item(hypogenCoil, 64), item(hypogenCoil, 64),
                        form(TagPrefix.wireGtOctal, superconductorUIV(), 32),
                        component(GodforgeItems.NEUTRONIUM_HEAT_CAPACITOR, 2),
                        component(GodforgeItems.SPACE_COOLANT_CELL, 2), tunnel(),
                        GTMultiMachines.LARGE_PLASMA_TURBINE.asStack(4), form(TagPrefix.plateDense, creon(), 6),
                        form(TagPrefix.plate, GTOMaterials.Hypogen, 6)),
                null, 0, List.of(solder(16 * INGOTS), molten(superconductorUIV(), 32 * INGOTS), fluid(dtec, 128_000)),
                item(siphon, 1), 10 * SECONDS, uiv);

        // Gravitational Lens
        var lens = item(GTBlocks.FUSION_GLASS.get(), 1);
        assemblyLine("gravitational_lens", lens,
                List.of(item(GTBlocks.FUSION_GLASS.get(), 8), item(GTBlocks.CASING_LAMINATED_GLASS.get(), 8),
                        item(GTOBlocks.FORCE_FIELD_GLASS.get(), 8), component(boson, 4),
                        form(TagPrefix.lens, GTOMaterials.MagnetoResonatic, 12),
                        form(TagPrefix.lens, GTMaterials.Diamond, 6),
                        form(TagPrefix.lens, GTMaterials.NetherStar, 6),
                        form(TagPrefix.plateDense, GTOMaterials.ChromaticGlass, 36),
                        form(TagPrefix.rodLong, creon(), 6), form(TagPrefix.rodLong, mellion(), 6),
                        form(TagPrefix.rodLong, sixPhasedCopper(), 6)),
                null, 0, List.of(molten(GTOMaterials.Rhugnor, 16 * INGOTS), molten(creon(), 16 * INGOTS),
                        molten(GTOMaterials.NitinolA, 16 * STACKS)),
                item(GodforgeBlocks.GRAVITATIONAL_LENS.get(), 1), 10 * SECONDS, uiv);

        // Graviton Flow Modulators (remote, then medial and central from the previous one)
        var anomaly = component(GodforgeItems.GRAVITON_ANOMALY, 1);
        assemblyLine("remote_graviton_flow_modulator", anomaly,
                List.of(item(confinement, 2), item(GTOBlocks.ADVANCED_FUSION_COIL.get(), 1),
                        form(TagPrefix.plate, creon(), 16), form(TagPrefix.gearSmall, mellion(), 8),
                        component(GodforgeItems.GRAVITON_ANOMALY, 2),
                        item(GTOItems.NON_LINEAR_OPTICAL_LENS.get(), 4),
                        item(GTItems.EMITTER_UIV.get(), 4),
                        ChemicalHelper.get(GTOTagPrefix.NANITES, GTMaterials.Silver, 2)),
                CustomTags.UEV_CIRCUITS, 16,
                List.of(solder(32 * INGOTS), molten(superconductorUIV(), 32 * INGOTS),
                        molten(GTOMaterials.Infinity, 32 * INGOTS)),
                item(GodforgeBlocks.REMOTE_GRAVITON_FLOW_MODULATOR.get(), 2), 10 * SECONDS, uiv);

        // Harmonic Phonon Transmission Conduit
        assemblyLine("harmonic_phonon_transmission_conduit", component(relativistic, 1),
                List.of(form(TagPrefix.frameGt, transcendent, 1), form(TagPrefix.rodLong, creon(), 12),
                        machine(GTMachines.QUANTUM_TANK, GTValues.UHV, 1), component(GodforgeItems.TESSERACT, 8),
                        component(relativistic, 4), component(GodforgeItems.THERMAL_SUPERCONDUCTOR, 6),
                        item(GTItems.FIELD_GENERATOR_UEV.get(), 4), form(TagPrefix.bolt, sixPhasedCopper(), 24)),
                null, 0, List.of(solder(STACKS), phonon(1_000), plasma(GTMaterials.Plutonium241, 16 * INGOTS)),
                item(conduit, 1), 10 * SECONDS, uiv);

        components();

        // Module controllers
        var hearth = GCYMMachines.MEGA_BLAST_FURNACE.asStack();
        module("godforge_smelting_module", hearth,
                List.of(item(shielding, 4), GCYMMachines.MEGA_BLAST_FURNACE.asStack(64),
                        GTMultiMachines.MULTI_SMELTER.asStack(64), battery(1),
                        form(TagPrefix.wireGtHex, superconductorUIV(), 16), item(GTItems.ROBOT_ARM_UIV.get(), 16),
                        item(GTItems.CONVEYOR_MODULE_UIV.get(), 32), form(TagPrefix.plateDense, sixPhasedCopper(), 16),
                        form(TagPrefix.plateDense, creon(), 8), form(TagPrefix.plateDense, mellion(), 8)),
                32, plasma(GTMaterials.Lead, 4 * STACKS), GodforgeMachines.GODFORGE_SMELTING_MODULE.asStack());
        module("godforge_molten_module", GCYMMachines.MEGA_ALLOY_BLAST_SMELTER.asStack(),
                List.of(item(shielding, 4), GCYMMachines.MEGA_BLAST_FURNACE.asStack(64),
                        GCYMMachines.MEGA_ALLOY_BLAST_SMELTER.asStack(64), battery(1),
                        form(TagPrefix.wireGtHex, superconductorUIV(), 32), item(GTItems.ROBOT_ARM_UIV.get(), 16),
                        item(GTItems.CONVEYOR_MODULE_UIV.get(), 32), item(GTItems.ELECTRIC_PUMP_UIV.get(), 64),
                        component(relativistic, 8), form(TagPrefix.plateDense, sixPhasedCopper(), 16),
                        form(TagPrefix.plateDense, creon(), 8), form(TagPrefix.plateDense, mellion(), 8)),
                32, phonon(32_000), GodforgeMachines.GODFORGE_MOLTEN_MODULE.asStack());
        var heater = machine(GTMachines.FLUID_HEATER, GTValues.UIV, 1);
        module("godforge_plasma_module", heater,
                List.of(item(shielding, 4), machine(GTMachines.FLUID_HEATER, GTValues.UIV, 64), advancedFusion(8),
                        battery(1), form(TagPrefix.wireGtHex, superconductorUIV(), 32),
                        item(GTItems.ROBOT_ARM_UIV.get(), 16), item(GTItems.CONVEYOR_MODULE_UIV.get(), 32),
                        item(GTItems.ELECTRIC_PUMP_UIV.get(), 64), component(relativistic, 8),
                        form(TagPrefix.plateDense, sixPhasedCopper(), 32),
                        form(TagPrefix.plateDense, creon(), 16),
                        form(TagPrefix.plateDense, mellion(), 16)),
                32, phonon(32_000), GodforgeMachines.GODFORGE_PLASMA_MODULE.asStack());
        module("godforge_exotic_module", plasmaMixer(1),
                List.of(item(shielding, 4), plasmaMixer(4), advancedFusion(1), battery(4),
                        form(TagPrefix.wireGtHex, superconductorUIV(), 64), item(GTItems.ROBOT_ARM_UIV.get(), 16),
                        item(GTItems.CONVEYOR_MODULE_UIV.get(), 32), item(GTItems.ELECTRIC_PUMP_UIV.get(), 64),
                        item(conduit, 8),
                        form(TagPrefix.plateDense, sixPhasedCopper(), 64),
                        form(TagPrefix.plateDense, creon(), 32),
                        form(TagPrefix.plateDense, mellion(), 32)),
                64, phonon(64_000), GodforgeMachines.GODFORGE_EXOTIC_MODULE.asStack());
    }

    private static void module(String id, ItemStack research, List<ItemStack> items, int circuits, FluidStack third,
                               ItemStack output) {
        assemblyLine(id, research, items, CustomTags.UIV_CIRCUITS, circuits,
                List.of(solder(16 * STACKS), fluid(GTOMaterials.ExcitedDtec, 2_048_000), third,
                        molten(GTOMaterials.TranscendentMetal, 16 * STACKS)),
                output, 300 * SECONDS, GTValues.VA[GTValues.UXV]);
    }

    /** Nanites of a material; GTO only generates some, the rest fall back to Cosmic Neutronium nanites (EOH tier). */
    private static ItemStack nanites(Material material, int amount) {
        ItemStack stack = ChemicalHelper.get(GTOTagPrefix.NANITES, material, amount);
        return stack.isEmpty() ? ChemicalHelper.get(GTOTagPrefix.NANITES, GTOMaterials.CosmicNeutronium, amount) : stack;
    }

    /** GTNA superdense plate (64 plates) as dense plates (9 plates each), capped at a stack. */
    private static ItemStack superdense(Material material, int amount) {
        return form(TagPrefix.plateDense, material, Math.min(64, amount * 7));
    }

    private static FluidStack stacks(Material material, int stacks) {
        return molten(material, stacks * STACKS);
    }

    /**
     * GTNA {@code GTNAGodforgeChainRecipes}: the Forge parts outside the GTNH Research Station line (shielding
     * casing, medial/central modulators) and the GTNH crafting components GTO has no counterpart for.
     */
    private static void components() {
        long uhv = GTValues.VA[GTValues.UHV], uev = GTValues.VA[GTValues.UEV], uiv = GTValues.VA[GTValues.UIV];
        long uxv = GTValues.VA[GTValues.UXV], opv = GTValues.VA[GTValues.OpV];
        var confinement = GodforgeBlocks.MAGNETIC_CONFINEMENT_CASING.get();
        var coil = GTOBlocks.ADVANCED_FUSION_COIL.get();
        var anomaly = GodforgeItems.GRAVITON_ANOMALY;
        var lens = GTOItems.NON_LINEAR_OPTICAL_LENS.get();

        gt(com.gtocore.common.data.GTORecipeTypes.ASSEMBLY_LINE_RECIPES, GTOAdditions.id("godforge/singularity_shielding_casing"))
                .inputItems(form(TagPrefix.frameGt, sixPhasedCopper(), 4))
                .inputItems(TagPrefix.plateDense, GTMaterials.Neutronium, 64)
                .inputItems(form(TagPrefix.plate, GTOMaterials.QuantumMetal, 16))
                .inputItems(form(TagPrefix.frameGt, GTOMaterials.Infinity, 4))
                .inputItems(item(GTItems.GRAVI_STAR.get(), 8))
                .inputItems(form(TagPrefix.rodLong, superconductorUIV(), 16))
                .inputItems(form(TagPrefix.plate, creon(), 16)).inputItems(form(TagPrefix.plate, mellion(), 16))
                .inputItems(superdense(GTOMaterials.TranscendentMetal, 2))
                .inputItems(TagPrefix.frameGt, GTMaterials.Tritanium, 4)
                .inputItems(form(TagPrefix.plate, sixPhasedCopper(), 16))
                .inputItems(TagPrefix.frameGt, GTMaterials.Naquadria, 4)
                .inputFluids(molten(GTOMaterials.Hypogen, 4 * INGOTS)).inputFluids(stacks(GTOMaterials.Infinity, 2))
                .inputFluids(stacks(GTOMaterials.CelestialTungsten, 32))
                .inputFluids(stacks(GTMaterials.Neutronium, 32))
                .outputItems(item(GodforgeBlocks.SINGULARITY_SHIELDING_CASING.get(), 6)).duration(6000).EUt(uxv)
                .addCondition(research(item(confinement, 1), CWUT, TOTAL_CWU, uxv)).save();
        gt(com.gtocore.common.data.GTORecipeTypes.ASSEMBLY_LINE_RECIPES, GTOAdditions.id("godforge/medial_graviton_flow_modulator"))
                .inputItems(item(confinement, 1)).inputItems(item(coil, 2))
                .inputItems(form(TagPrefix.plateDense, creon(), 8))
                .inputItems(form(TagPrefix.gear, mellion(), 4)).inputItems(form(TagPrefix.frameGt, superconductorUIV(), 32))
                .inputItems(component(anomaly, 4)).inputItems(item(lens, 8))
                .inputItems(item(GTItems.EMITTER_UXV.get(), 4))
                .inputItems(CustomTags.UXV_CIRCUITS, 8)
                .inputItems(nanites(GTMaterials.Silver, 2)).inputItems(nanites(GTMaterials.Gold, 2))
                .inputFluids(molten(GTOMaterials.ChromaticGlass, 64 * INGOTS))
                .inputFluids(molten(GTOMaterials.Infinity, 32 * INGOTS))
                .inputFluids(molten(GTOMaterials.TranscendentMetal, 32 * INGOTS))
                .outputItems(item(GodforgeBlocks.MEDIAL_GRAVITON_FLOW_MODULATOR.get(), 1))
                .duration(6000).EUt(uxv)
                .addCondition(research(item(GodforgeBlocks.REMOTE_GRAVITON_FLOW_MODULATOR.get(), 1), CWUT, TOTAL_CWU, uxv))
                .save();
        gt(com.gtocore.common.data.GTORecipeTypes.ASSEMBLY_LINE_RECIPES, GTOAdditions.id("godforge/central_graviton_flow_modulator"))
                .inputItems(item(confinement, 1)).inputItems(item(coil, 8)).inputItems(superdense(creon(), 8))
                .inputItems(form(TagPrefix.gear, mellion(), 64)).inputItems(component(anomaly, 8))
                .inputItems(item(lens, 8))
                .inputItems(CustomTags.MAX_CIRCUITS, 4)
                .inputItems(item(GTItems.EMITTER_OpV.get(), 4))
                .inputItems(nanites(GTMaterials.Silver, 8)).inputItems(nanites(GTMaterials.Gold, 8))
                .inputItems(nanites(sixPhasedCopper(), 8)).inputItems(nanites(universium(), 8))
                .inputFluids(molten(GTOMaterials.ChromaticGlass, 256 * INGOTS))
                .inputFluids(molten(GTOMaterials.Infinity, 32 * INGOTS)).inputFluids(solder(20_000))
                .inputFluids(molten(GTOMaterials.Eternity, 128 * INGOTS))
                .outputItems(item(GodforgeBlocks.CENTRAL_GRAVITON_FLOW_MODULATOR.get(), 1))
                .duration(6000).EUt(opv)
                .addCondition(research(item(GodforgeBlocks.MEDIAL_GRAVITON_FLOW_MODULATOR.get(), 1), MODULE_CWUT, 512_000_000, opv))
                .save();

        var tesseract = GodforgeItems.TESSERACT;
        var boson = GodforgeItems.STABLE_BOSON_CONTAINMENT_UNIT;
        var composite = GodforgeItems.SUPERCONDUCTOR_COMPOSITE;
        var thermal = GodforgeItems.THERMAL_SUPERCONDUCTOR;
        gt(com.gtocore.common.data.GTORecipeTypes.ASSEMBLY_LINE_RECIPES, GTOAdditions.id("godforge/tesseract"))
                .inputItems(form(TagPrefix.frameGt, GTOMaterials.Infinity, 1))
                .inputItems(item(GTItems.QUANTUM_STAR.get(), 4))
                .inputItems(item(GTItems.FIELD_GENERATOR_UEV.get(), 2))
                .inputItems(TagPrefix.plateDense, GTMaterials.Neutronium, 4)
                .inputFluids(molten(GTOMaterials.CelestialTungsten, 576)).inputFluids(molten(mellion(), 1152))
                .outputItems(component(tesseract, 1)).duration(1200).EUt(uiv)
                .addCondition(research(item(GTItems.QUANTUM_STAR.get(), 1), 4096, 64_000_000, uiv))
                .save();
        gt(com.gtocore.common.data.GTORecipeTypes.ASSEMBLER_RECIPES, GTOAdditions.id("godforge/stable_boson_containment_unit"))
                .inputItems(item(GTItems.GRAVI_STAR.get(), 1)).inputItems(item(GTItems.FIELD_GENERATOR_UHV.get(), 1))
                .inputItems(TagPrefix.plate, GTMaterials.Neutronium, 4).inputFluids(GTMaterials.Krypton, 100)
                .outputItems(component(boson, 1)).duration(200).EUt(uhv).save();
        gt(com.gtocore.common.data.GTORecipeTypes.ASSEMBLER_RECIPES, GTOAdditions.id("godforge/superconductor_composite"))
                .inputItems(TagPrefix.dust, GTMaterials.Samarium, 8)
                .inputItems(TagPrefix.dust, GTMaterials.Neodymium, 8)
                .inputItems(TagPrefix.wireFine, GTMaterials.RutheniumTriniumAmericiumNeutronate, 16)
                .inputFluids(GTMaterials.Cerium, 576)
                .outputItems(component(composite, 1)).duration(400).EUt(uhv).save();
        gt(com.gtocore.common.data.GTORecipeTypes.ASSEMBLER_RECIPES, GTOAdditions.id("godforge/electromagnet_tengam"))
                .inputItems(form(TagPrefix.rodLong, GTOMaterials.AttunedTengam, 4))
                .inputItems(form(TagPrefix.frameGt, GTOMaterials.Infinity, 1))
                .inputItems(form(TagPrefix.wireFine, superconductorUIV(), 32))
                .outputItems(component(GodforgeItems.ELECTROMAGNET_TENGAM, 1)).duration(400).EUt(uev).save();
        gt(com.gtocore.common.data.GTORecipeTypes.ASSEMBLER_RECIPES, GTOAdditions.id("godforge/neutronium_heat_capacitor"))
                .inputItems(TagPrefix.plateDense, GTMaterials.Neutronium, 8)
                .inputItems(TagPrefix.rodLong, GTMaterials.Neutronium, 4)
                .outputItems(component(GodforgeItems.NEUTRONIUM_HEAT_CAPACITOR, 1))
                .duration(100).EUt(GTValues.VA[GTValues.ZPM]).save();
        gt(com.gtocore.common.data.GTORecipeTypes.ASSEMBLER_RECIPES, GTOAdditions.id("godforge/space_coolant_cell"))
                .inputItems(TagPrefix.plate, GTMaterials.Tritanium, 6)
                .inputItems(TagPrefix.plate, GTMaterials.Neutronium, 2)
                .inputFluids(GTMaterials.Helium.getFluid(FluidStorageKeys.LIQUID, 1000))
                .outputItems(component(GodforgeItems.SPACE_COOLANT_CELL, 1)).duration(400).EUt(uhv).save();
        gt(com.gtocore.common.data.GTORecipeTypes.ASSEMBLER_RECIPES, GTOAdditions.id("godforge/thermal_superconductor"))
                .inputItems(form(TagPrefix.frameGt, GTOMaterials.TranscendentMetal, 1))
                .inputItems(form(TagPrefix.foil, superconductorUIV(), 64))
                .inputItems(TagPrefix.plate, GTMaterials.Neutronium, 32)
                .inputItems(item(GTItems.ELECTRIC_PUMP_UIV.get(), 1))
                .inputFluids(phonon(100))
                .outputItems(component(thermal, 1)).duration(40).EUt(uiv).save();
        gt(com.gtocore.common.data.GTORecipeTypes.ASSEMBLER_RECIPES, GTOAdditions.id("godforge/relativistic_heat_capacitor"))
                .inputItems(component(thermal, 2)).inputItems(TagPrefix.plate, GTMaterials.Naquadria, 16)
                .inputItems(form(TagPrefix.plate, superconductorUIV(), 8))
                .inputItems(form(TagPrefix.gear, sixPhasedCopper(), 3))
                .inputItems(item(GTItems.FIELD_GENERATOR_UEV.get(), 1))
                .inputFluids(phonon(500))
                .outputItems(component(GodforgeItems.RELATIVISTIC_HEAT_CAPACITOR, 1)).duration(100).EUt(uiv).save();
        gt(com.gtocore.common.data.GTORecipeTypes.ASSEMBLY_LINE_RECIPES, GTOAdditions.id("godforge/graviton_anomaly"))
                .inputItems(item(GTItems.QUANTUM_STAR.get(), 1)).inputItems(component(boson, 2))
                .inputItems(TagPrefix.plate, GTMaterials.Naquadria, 16)
                .inputItems(CustomTags.UHV_CIRCUITS, 8)
                .inputItems(TagPrefix.wireGtHex, GTMaterials.RutheniumTriniumAmericiumNeutronate, 32)
                .inputItems(form(TagPrefix.bolt, GTOMaterials.AstralTitanium, 8))
                .inputItems(TagPrefix.screw, GTMaterials.Tritanium, 8)
                .inputFluids(GTMaterials.Naquadria, 16 * INGOTS)
                .inputFluids(GTMaterials.Trinium, 32 * INGOTS)
                .inputFluids(GTMaterials.Osmiridium, 16 * INGOTS)
                .inputFluids(GTMaterials.Tritanium, 16 * INGOTS)
                .outputItems(component(GodforgeItems.GRAVITON_ANOMALY, 1))
                .duration(2400).EUt(uhv)
                .addCondition(research(component(boson, 1), 2048, 32_000_000, uev))
                .save();
    }

    // ------------------------------------------------------------------ upgrade extra costs

    private static boolean costsRegistered;

    /** GTNH {@code Godforge.java} extra costs (GTNA {@code registerExtraCosts}) with GTO substitutes. Idempotent. */
    public static synchronized void registerExtraCosts() {
        if (costsRegistered) return;
        costsRegistered = true;
        var siphon = GodforgeBlocks.STELLAR_ENERGY_SIPHON_CASING.get();
        var conduit = GodforgeBlocks.HARMONIC_PHONON_TRANSMISSION_CONDUIT.get();
        var boundary = GTOBlocks.DIMENSIONAL_STABILITY_CASING.get();
        var fields = GTOBlocks.SPACETIME_COMPRESSION_FIELD_GENERATOR.get();
        cost(GodforgeUpgrade.START, form(TagPrefix.frameGt, superconductorUIV(), 64),
                component(GodforgeItems.SUPERCONDUCTOR_COMPOSITE, 32),
                form(TagPrefix.gear, metastableOganesson(), 16), singularity(8),
                item(GTItems.ROBOT_ARM_UIV.get(), 64), item(GTItems.FIELD_GENERATOR_UEV.get(), 64));
        cost(GodforgeUpgrade.FDIM, GCYMMachines.MEGA_ALLOY_BLAST_SMELTER.asStack(16),
                item(GodforgeBlocks.HYPOGEN_COIL.get(), 64), item(conduit, 32), singularity(16),
                item(GTOBlocks.ADVANCED_FUSION_COIL.get(), 48), item(GTItems.ROBOT_ARM_UIV.get(), 64),
                item(GTItems.FIELD_GENERATOR_UEV.get(), 64));
        cost(GodforgeUpgrade.GPCI, item(siphon, 8), advancedFusion(8), item(GTOBlocks.FUSION_CASING_MK4.get(), 64),
                form(TagPrefix.foil, GTMaterials.Neutronium, 64),
                form(TagPrefix.plateDense, GTOMaterials.QuantumMetal, 48),
                form(TagPrefix.gear, rhugnorSolid(), 32), singularity(16),
                item(GTItems.ROBOT_ARM_UIV.get(), 64), item(GTItems.FIELD_GENERATOR_UEV.get(), 64));
        cost(GodforgeUpgrade.QGPIU, item(siphon, 16), advancedFusion(2),
                item(GTOBlocks.COMPRESSED_FUSION_COIL_MK2.get(), 64), item(conduit, 16), plasmaMixer(4),
                form(TagPrefix.gear, rhugnorSolid(), 64), form(TagPrefix.gear, ichorium(), 64), singularity(32),
                item(GTItems.ROBOT_ARM_UIV.get(), 64), item(GTItems.FIELD_GENERATOR_UEV.get(), 64));
        cost(GodforgeUpgrade.CD, form(TagPrefix.frameGt, GTOMaterials.SpaceTime, 64),
                form(TagPrefix.frameGt, superconductorUMV(), 64), form(TagPrefix.frameGt, GTOMaterials.Hypogen, 64),
                form(TagPrefix.frameGt, dragonMetal(), 64), item(GTOBlocks.DIMENSION_INJECTION_CASING.get(), 64),
                item(boundary, 8), megaBattery(2), item(GTItems.FIELD_GENERATOR_UXV.get(), 32));
        cost(GodforgeUpgrade.EE, form(TagPrefix.frameGt, GTOMaterials.WhiteDwarfMatter, 64),
                form(TagPrefix.frameGt, GTOMaterials.BlackDwarfMatter, 64),
                form(TagPrefix.frameGt, GTOMaterials.Eternity, 16), form(TagPrefix.frameGt, universium(), 2),
                item(boundary, 64), item(fields, 48), megaBattery(16),
                GodforgeMachines.GODFORGE_EXOTIC_MODULE.asStack(32), item(siphon, 64),
                item(GTItems.FIELD_GENERATOR_UXV.get(), 64), item(GTItems.ROBOT_ARM_UXV.get(), 64));
        cost(GodforgeUpgrade.END,
                form(TagPrefix.frameGt, GTOMaterials.MagnetohydrodynamicallyConstrainedStarMatter, 64),
                form(TagPrefix.frameGt, GTOMaterials.Eternity, 64), form(TagPrefix.frameGt, GTOMaterials.Magmatter, 64),
                item(fields, 64), GodforgeMachines.GODFORGE_EXOTIC_MODULE.asStack(64),
                MultiBlockD.EYE_OF_HARMONY.asStack(1), megaBattery(32), item(GTItems.FIELD_GENERATOR_UXV.get(), 64),
                item(GTItems.ROBOT_ARM_UXV.get(), 64));
    }

    private static void cost(GodforgeUpgrade upgrade, ItemStack... stacks) {
        upgrade.clearExtraCost();
        List<GodforgeUpgrade.ExtraCost> costs = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty() || costs.size() >= 12) continue;
            costs.add(new GodforgeUpgrade.ExtraCost(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),
                    stack.getCount()));
        }
        upgrade.addExtraCost(costs.toArray(GodforgeUpgrade.ExtraCost[]::new));
    }
}
