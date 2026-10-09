package com.raishxn.gtoa.godforge;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.handler.RecipeHandlerUnit;
import com.gregtechceu.gtceu.api.recipe.modifier.ParallelLogic;
import com.gregtechceu.gtceu.common.data.GTRecipeDataKeys;
import com.gtolib.api.machine.multiblock.CrossRecipeMultiblockMachine;

import com.gto.datasynclib.annotations.SaveToDisk;
import com.gto.datasynclib.annotations.SyncToClient;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;

import com.raishxn.gtoa.godforge.api.GodforgeModuleStats;
import com.raishxn.gtoa.godforge.api.GodforgeModuleStats.Type;
import com.raishxn.gtoa.godforge.api.GodforgeOverclock;
import org.jetbrains.annotations.Nullable;

import java.math.BigInteger;
import java.util.UUID;
import java.util.function.Supplier;

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;

/**
 * Forge of Gods module, ported from GTNA ({@code GodforgeModuleMachine}, after GTNH {@code MTEBaseModule} and its
 * Smelting/Molten/Plasma subclasses). It has no energy hatches: the whole recipe energy is taken from the owner's
 * wireless network when the recipe starts, with heat, parallels, speed and voltage pushed by the forge every five
 * seconds.
 */
public class GodforgeModuleMachine extends CrossRecipeMultiblockMachine implements IGodforgeModule {

    private final GodforgeModuleStats stats;

    @SaveToDisk
    @SyncToClient
    private boolean connected;
    @SaveToDisk
    @Nullable
    private UUID networkOwner;
    /** GTNH voltage config panel: parallel cap and "always max parallel". */
    @SaveToDisk
    @SyncToClient
    private int setMaxParallel = 1;
    @SaveToDisk
    @SyncToClient
    private boolean alwaysMaxParallel = true;

    // Client mirrors of the forge-pushed stats shown in the GUI.
    @SyncToClient
    private CompoundTag syncedStats = new CompoundTag();

    /** Why the last recipe found could not start (empty when it started), shown in the GUI and logged on change. */
    @SyncToClient
    private String blockedReason = "";

    private GTRecipe blocked(String reason, String detail) {
        String full = detail.isEmpty() ? reason : reason + ":" + detail;
        if (!full.equals(blockedReason)) {
            blockedReason = full;
            com.mojang.logging.LogUtils.getLogger().info("Godforge module {} at {} cannot start: {}", stats.type,
                    getPos(), full);
            if (getLevel() instanceof net.minecraft.server.level.ServerLevel server) {
                server.getServer().execute(this::requestSync);
            }
        }
        return null;
    }

    /** Energy priced by {@link #getRealRecipe} for a recipe, debited when it starts. */
    private final java.util.Map<GTRecipe, long[]> pending = new java.util.IdentityHashMap<>();
    private final java.util.Map<GTRecipe, BigInteger> pendingEU = new java.util.IdentityHashMap<>();

    public GodforgeModuleMachine(MetaMachineBlockEntity holder, Type type) {
        // GTO's cross-recipe base runs one recipe per Thread Hatch thread. Parallels come from the forge (see
        // getRealRecipe), so the base's own parallel is 1.
        super(holder, false, false, m -> 1L);
        this.stats = new GodforgeModuleStats(type);
    }

    /** No energy hatches: the recipe energy is paid from the wireless network, so no voltage cap applies here. */
    @Override
    public long getOverclockVoltage() {
        return Long.MAX_VALUE;
    }

    @Override
    public boolean useEnergy(long eu, boolean simulate) {
        return true;
    }

    // ---- GTO Thread Hatch: gated by forge upgrades (GPCI/CD/END), as GTNA gates its own Thread Hatch ----

    @Nullable
    private com.gregtechceu.gtceu.api.machine.feature.ITieredMachine threadPart() {
        for (var part : getParts()) {
            if (part instanceof com.gtolib.api.machine.impl.part.ThreadPartMachine thread) return thread;
        }
        return null;
    }

    public boolean threadHatchUnlocked() {
        var part = threadPart();
        return part == null || part.getTier() <= displayStats().maxThreadHatchTier;
    }

    @Override
    public GodforgeModuleStats godforgeStats() {
        return stats;
    }

    public Type type() {
        return stats.type;
    }

    @Nullable
    protected UUID networkOwner() {
        return networkOwner;
    }

    @Override
    public void setNetworkOwner(UUID owner) {
        networkOwner = owner;
    }

    @Override
    public void connect() {
        stats.connected = true;
        if (!connected) {
            connected = true;
            requestSync();
        }
    }

    @Override
    public void disconnect() {
        stats.connected = false;
        if (connected) {
            connected = false;
            requestSync();
        }
    }

    public boolean isConnected() {
        return connected;
    }

    /** {@code getActualParallel}: the forge's maximum, or the configured cap below it. */
    public int actualParallel() {
        int max = Math.max(1, displayStats().calculatedMaxParallel);
        return alwaysMaxParallel ? max : Math.max(1, Math.min(max, setMaxParallel));
    }

    public void setMaxParallel(int parallel) {
        setMaxParallel = Math.max(1, parallel);
        onChanged();
    }

    public void toggleAlwaysMaxParallel() {
        alwaysMaxParallel = !alwaysMaxParallel;
        onChanged();
    }

    /** With The Boundless Flow the processing voltage is chosen per module. */
    public void setProcessingVoltage(long voltage) {
        if (!stats.voltageConfig) return;
        stats.processingVoltage = Math.max(2_000_000_000L, voltage);
        onChanged();
    }

    // ---- GUI (GTNH MTEBaseModuleGui) ----

    @Override
    public void attachSideTabs(com.gregtechceu.gtceu.api.gui.fancy.TabsWidget sideTabs) {
        super.attachSideTabs(sideTabs);
        sideTabs.attachSubTab(new com.raishxn.gtoa.godforge.gui.GodforgePage(
                "gtoa.godforge.gui.voltage_config", com.gregtechceu.gtceu.api.gui.GuiTextures.BUTTON_POWER
                        .getSubTexture(0, 0, 1, 0.5),
                this::voltagePage));
    }

    private com.lowdragmc.lowdraglib.gui.widget.Widget voltagePage() {
        var page = new com.lowdragmc.lowdraglib.gui.widget.WidgetGroup(0, 0, 200, 120);
        page.setBackground(com.raishxn.gtoa.godforge.gui.GodforgeGui.BACKGROUND_STAR);
        page.addWidget(new com.lowdragmc.lowdraglib.gui.widget.ImageWidget(5, 5, 190, 10,
                com.raishxn.gtoa.godforge.gui.GodforgeGui.text("gtoa.godforge.gui.parallel",
                        0xFFAA00)));
        page.addWidget(
                new com.raishxn.gtoa.godforge.gui.GodforgeGui.ServerField(10, 18, 120, 14,
                        () -> String.valueOf(setMaxParallel), value -> {
                            try {
                                setMaxParallel(Integer.parseInt(value.trim()));
                            } catch (NumberFormatException ignored) {}
                        }).setNumbersOnly(1, Integer.MAX_VALUE));
        page.addWidget(new com.lowdragmc.lowdraglib.gui.widget.SwitchWidget(136, 17, 54, 16,
                (click, pressed) -> {
                    if (!click.isRemote) toggleAlwaysMaxParallel();
                }).setSupplier(() -> alwaysMaxParallel)
                .setTexture(new com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup(
                        com.gregtechceu.gtceu.api.gui.GuiTextures.BUTTON,
                        new com.lowdragmc.lowdraglib.gui.texture.TextTexture("gtoa.godforge.gui.max_parallel")),
                        new com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup(
                                com.raishxn.gtoa.godforge.gui.GodforgeGui.BUTTON_SPACE_PRESSED,
                                new com.lowdragmc.lowdraglib.gui.texture.TextTexture(
                                        "gtoa.godforge.gui.max_parallel")))
                .setHoverTooltips("gtoa.godforge.gui.max_parallel.tooltip"));
        page.addWidget(new com.lowdragmc.lowdraglib.gui.widget.ImageWidget(5, 40, 190, 10,
                com.raishxn.gtoa.godforge.gui.GodforgeGui.text(
                        "gtoa.godforge.gui.voltageinfo",
                        0xFFAA00)));
        page.addWidget(
                new com.raishxn.gtoa.godforge.gui.GodforgeGui.ServerField(10, 53, 180, 14,
                        () -> String.valueOf(displayStats().processingVoltage), value -> {
                            try {
                                setProcessingVoltage(Long.parseLong(value.trim()));
                            } catch (NumberFormatException ignored) {}
                        }).setNumbersOnly(2_000_000_000L, Long.MAX_VALUE)
                        .setHoverTooltips(net.minecraft.network.chat.Component.translatable(
                                "gtoa.godforge.text.tooltip.voltageadjustment"),
                                net.minecraft.network.chat.Component.translatable(
                                        "gtoa.godforge.text.tooltip.voltageadjustment.1")));
        page.addWidget(new com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget(10, 74, lines -> {
            if (!displayStats().voltageConfig) {
                lines.add(net.minecraft.network.chat.Component.translatable(
                        "gtoa.godforge.button.voltageconfig.tooltip.02").withStyle(net.minecraft.ChatFormatting.RED));
            }
        }).setMaxWidthLimit(180));
        return page;
    }

    @Override
    public void customText(java.util.List<net.minecraft.network.chat.Component> text) {
        super.customText(text);
        if (!isFormed()) return;
        GodforgeModuleStats stats = displayStats();
        text.add(net.minecraft.network.chat.Component.translatable("gtoa.godforge.gui.modulestatus").append(" ")
                .append(net.minecraft.network.chat.Component.translatable("gtoa.godforge.gui.modulestatus." +
                        connected).withStyle(connected ? net.minecraft.ChatFormatting.GREEN :
                                net.minecraft.ChatFormatting.RED)));
        if (stats.type != Type.EXOTIC) {
            stat(text, "heat", String.valueOf(stats.heat));
            stat(text, "effectiveheat", String.valueOf(stats.heatForOC));
        }
        stat(text, "parallel", String.valueOf(actualParallel()));
        if (!blockedReason.isEmpty()) {
            String[] reason = blockedReason.split(":", 2);
            text.add(net.minecraft.network.chat.Component.translatableWithFallback(
                    "gtoa.godforge.gui.blocked." + reason[0], "Cannot start: " + reason[0],
                    reason.length > 1 ? reason[1] : "").withStyle(net.minecraft.ChatFormatting.RED));
        }
        var thread = threadPart();
        if (thread != null && !threadHatchUnlocked()) {
            int t = thread.getTier();
            String need = t > com.gregtechceu.gtceu.api.GTValues.OpV ? "END" :
                    t > com.gregtechceu.gtceu.api.GTValues.UIV ? "CD" : "GPCI";
            text.add(net.minecraft.network.chat.Component.translatable("gtoa.godforge.gui.threads.locked", need,
                    com.gregtechceu.gtceu.api.GTValues.VN[stats.maxThreadHatchTier])
                    .withStyle(net.minecraft.ChatFormatting.RED));
        }
        stat(text, "speedbonus", String.format(java.util.Locale.ROOT, "%.4f", stats.speedBonus));
        stat(text, "energydiscount", String.format(java.util.Locale.ROOT, "%.4f", stats.energyDiscount));
        stat(text, "ocdivisor", String.format(java.util.Locale.ROOT, "%.2f", stats.overclockTimeFactor));
        stat(text, "processingvoltage", com.gregtechceu.gtceu.utils.FormattingUtil.formatNumbers(
                stats.processingVoltage) + " EU/t");
        if (stats.type == Type.PLASMA) {
            text.add(net.minecraft.network.chat.Component.translatable("gtoa.godforge.gui.plasmarecipetier",
                    stats.plasmaTier));
            text.add(net.minecraft.network.chat.Component.translatable("gtoa.godforge.gui.plasmamultistep",
                    stats.multiStepPlasma));
        }
    }

    private static void stat(java.util.List<net.minecraft.network.chat.Component> text, String key, String value) {
        text.add(net.minecraft.network.chat.Component.translatable("gtoa.godforge.gui." + key).append(": ")
                .append(net.minecraft.network.chat.Component.literal(value)
                        .withStyle(net.minecraft.ChatFormatting.GOLD)));
    }

    // ---- Structure: 7x7x13, GTNH MTEBaseModule ----

    private static final String[][] SHAPE = {
            { "       ", "  BBB  ", " BBBBB ", " BB~BB ", " BBBBB ", "  BBB  ", "       " },
            { "  CCC  ", " CFFFC ", "CFFFFFC", "CFFFFFC", "CFFFFFC", " CFFFC ", "  CCC  " },
            { "       ", "       ", "   E   ", "  EAE  ", "   E   ", "       ", "       " },
            { "       ", "       ", "   E   ", "  EAE  ", "   E   ", "       ", "       " },
            { "       ", "       ", "   E   ", "  EAE  ", "   E   ", "       ", "       " },
            { "       ", "       ", "   E   ", "  EAE  ", "   E   ", "       ", "       " },
            { "       ", "       ", "   E   ", "  EAE  ", "   E   ", "       ", "       " },
            { "       ", "       ", "       ", "   D   ", "       ", "       ", "       " },
            { "       ", "       ", "       ", "   D   ", "       ", "       ", "       " },
            { "       ", "       ", "       ", "   D   ", "       ", "       ", "       " },
            { "       ", "       ", "       ", "   D   ", "       ", "       ", "       " },
            { "       ", "       ", "       ", "   D   ", "       ", "       ", "       " },
            { "       ", "       ", "       ", "   G   ", "       ", "       ", "       " } };

    /** Aisles in GTCEu's classic front-to-back, bottom-up order (StructureLib slices reversed). */
    public static String[][] aisles() {
        String[][] result = new String[SHAPE.length][];
        for (int slice = SHAPE.length - 1, i = 0; slice >= 0; slice--, i++) {
            String[] rows = new String[SHAPE[slice].length];
            for (int row = 0; row < rows.length; row++) rows[row] = SHAPE[slice][rows.length - 1 - row];
            result[i] = rows;
        }
        return result;
    }

    public static java.util.Map<Character, TraceabilityPredicate> symbols(MultiblockMachineDefinition definition, Type type) {
        Supplier<Block> core = type == Type.SMELTING ? GodforgeBlocks.HYPOGEN_COIL::get :
                GodforgeBlocks.HARMONIC_PHONON_TRANSMISSION_CONDUIT::get;
        var hatches = blocks(GodforgeBlocks.SINGULARITY_SHIELDING_CASING.get())
                .or(abilities(PartAbility.IMPORT_ITEMS))
                .or(abilities(PartAbility.IMPORT_FLUIDS))
                .or(abilities(PartAbility.EXPORT_ITEMS))
                .or(abilities(PartAbility.EXPORT_FLUIDS));
        if (type != Type.EXOTIC) {
            hatches = hatches.or(abilities(com.gtocore.api.machine.part.GTOPartAbility.THREAD_HATCH)
                    .setMaxGlobalLimited(1));
        }
        var map = new java.util.LinkedHashMap<Character, TraceabilityPredicate>();
        map.put('~', controller(definition));
        map.put('A', blocks(core.get()));
        map.put('B', hatches);
        map.put('C', blocks(GodforgeBlocks.SINGULARITY_SHIELDING_CASING.get()));
        map.put('D', blocks(GodforgeBlocks.GUIDANCE_CASING.get()));
        map.put('E', blocks(GodforgeBlocks.BOUNDLESS_STRUCTURE_CASING.get()));
        map.put('F', blocks(GodforgeBlocks.MAGNETIC_CONFINEMENT_CASING.get()));
        map.put('G', blocks(GodforgeBlocks.STELLAR_ENERGY_SIPHON_CASING.get()));
        map.put(' ', any());
        return map;
    }

    // ---- Recipes ----

    private boolean realRecipeReached;

    /** Diagnostics: GTO's base class can drop a recipe before {@link #getRealRecipe}; say so in the GUI and log. */
    @Nullable
    @Override
    public GTRecipe fullModifyRecipe(RecipeHandlerUnit unit, com.gregtechceu.gtceu.api.recipe.GTRecipeDefinition definition) {
        realRecipeReached = false;
        GTRecipe result = super.fullModifyRecipe(unit, definition);
        if (result == null && !realRecipeReached) blocked("base", String.valueOf(definition.id));
        return result;
    }

    /** Recipe heat, as GTCEu blast recipes store it. */
    private static int recipeHeat(GTRecipe recipe) {
        return recipe.data.getInt(GTRecipeDataKeys.EBF_TEMP);
    }

    /**
     * GT5 processing with the module stats: heat and voltage checks, parallels with the sub-tick multiplier,
     * GT5 overclocks (heat overclocks for Smelting/Molten). The resulting energy is checked against the wireless
     * network here and paid in {@link #beforeWorking}.
     */
    @Nullable
    @Override
    public GTRecipe getRealRecipe(RecipeHandlerUnit unit, GTRecipe recipe) {
        realRecipeReached = true;
        if (!connected) return blocked("disconnected", "");
        if (!threadHatchUnlocked()) return blocked("thread", "");
        long recipeEUt = Math.max(0, recipe.eut);
        int heat = recipeHeat(recipe);
        boolean heated = stats.type == Type.SMELTING || stats.type == Type.MOLTEN;
        if (heated && heat > stats.heat) return blocked("heat", heat + " > " + stats.heat);
        if (stats.type == Type.SMELTING && recipeEUt > stats.processingVoltage) {
            return blocked("voltage", recipeEUt + " > " + stats.processingVoltage);
        }
        if (stats.type == Type.PLASMA && (recipe.data.getInt(GodforgeRecipeTypes.PLASMA_TIER) > stats.plasmaTier ||
                recipe.data.getBoolean(GodforgeRecipeTypes.PLASMA_MULTISTEP) && !stats.multiStepPlasma)) {
            return blocked("plasma", "");
        }

        int maxParallel = actualParallel();
        long voltage = Math.min(stats.processingVoltage, Long.MAX_VALUE / maxParallel);
        GodforgeOverclock probe = calculator(stats, recipeEUt, recipe.duration, heat, voltage, heated);
        probe.parallel = maxParallel;
        int subTickParallel = (int) Math.min(Integer.MAX_VALUE, (long) (maxParallel * probe.multiplierUnderOneTick()));
        long parallel = ParallelLogic.getMaxParallelAmount(this, unit, recipe, Math.max(1, subTickParallel));
        if (parallel <= 0) return blocked("parallel", "");

        GodforgeOverclock calc = calculator(stats, recipeEUt, recipe.duration, heat, voltage, heated);
        calc.parallel = (int) Math.min(maxParallel, parallel);
        calc.calculate();
        if (calc.consumption() == Long.MAX_VALUE || calc.calculatedDuration() == Integer.MAX_VALUE) {
            return blocked("overflow", "");
        }
        BigInteger cost = BigInteger.valueOf(calc.consumption()).multiply(BigInteger.valueOf(calc.calculatedDuration()));
        if (networkOwner == null) return blocked("owner", "");
        if (GodforgeWireless.stored(networkOwner).compareTo(cost) < 0) {
            return blocked("energy", GodforgeWireless.stored(networkOwner) + " < " + cost);
        }
        blockedReason = "";

        recipe.modifier(parallel, false);
        recipe.duration = calc.calculatedDuration();
        // The energy was priced above and is paid from the wireless network, not from hatches.
        recipe.eut = 0;
        pending.put(recipe, new long[] { parallel, heat });
        pendingEU.put(recipe, cost);
        return recipe;
    }

    /**
     * Molten module: blast furnace outputs that have a molten (or other) fluid leave as that fluid, as in GTNH
     * {@code Godforge.initMoltenModuleRecipes} — 144 mB per ingot of material, 1000 mB for non-molten fluids.
     */
    public static void convertOutputsToMolten(java.util.List<com.gregtechceu.gtceu.api.recipe.content.Content<com.gregtechceu.gtceu.api.recipe.ingredient.ItemIngredient>> items,
                                              java.util.List<com.gregtechceu.gtceu.api.recipe.content.Content<com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient>> fluids) {
        var kept = new java.util.ArrayList<com.gregtechceu.gtceu.api.recipe.content.Content<com.gregtechceu.gtceu.api.recipe.ingredient.ItemIngredient>>();
        for (var content : items) {
            var stack = content.inner.getItem();
            net.minecraft.world.level.material.Fluid fluid = null;
            long amount = 0;
            if (!stack.isEmpty()) {
                var materialStack = com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper.getMaterialStack(stack);
                var material = materialStack.material();
                if (!materialStack.isEmpty() && material.hasFluid()) {
                    var molten = material.getFluid(com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys.MOLTEN);
                    var liquid = material.getFluid(com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys.LIQUID);
                    if (molten != null || liquid != null) {
                        fluid = molten != null ? molten : liquid;
                        amount = 144L * materialStack.amount() / com.gregtechceu.gtceu.api.GTValues.M * content.amount;
                    } else {
                        fluid = material.getFluid();
                        amount = 1000L * content.amount;
                    }
                }
            }
            if (fluid == null || amount <= 0) {
                kept.add(content);
                continue;
            }
            fluids.add(new com.gregtechceu.gtceu.api.recipe.content.Content<>(
                    com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient.of(fluid, amount), amount,
                    content.chance, content.tierChanceBoost));
        }
        items.clear();
        items.addAll(kept);
    }

    private static GodforgeOverclock calculator(GodforgeModuleStats stats, long recipeEUt, int duration, int heat,
                                                long voltage, boolean heated) {
        GodforgeOverclock calc = new GodforgeOverclock();
        calc.recipeEUt = recipeEUt;
        calc.duration = duration;
        calc.machineVoltage = voltage;
        calc.machineAmperage = Integer.MAX_VALUE;
        calc.durationModifier = stats.speedBonus;
        calc.eutModifier = stats.energyDiscount;
        calc.durationDecreasePerOC = stats.overclockTimeFactor;
        if (heated) {
            calc.recipeHeat = heat;
            calc.heatOC = true;
            calc.heatDiscount = true;
            calc.machineHeat = Math.max(heat, stats.heatForOC);
            calc.heatDiscountExponent = stats.heatEnergyDiscount();
        }
        return calc;
    }

    @Override
    public void beforeWorking(RecipeHandlerUnit unit, GTRecipe recipe) {
        BigInteger cost = pendingEU.remove(recipe);
        long[] info = pending.remove(recipe);
        pendingEU.clear();
        pending.clear();
        if (cost != null && networkOwner != null && cost.signum() > 0) GodforgeWireless.consume(networkOwner, cost);
        if (cost != null) stats.powerTally = stats.powerTally.add(cost);
        if (info != null) {
            stats.recipeTally += info[0];
            stats.currentRecipeHeat = info[1];
        }
        super.beforeWorking(unit, recipe);
    }

    /** The forge-pushed stats: live on the server, mirrored through {@link #syncedStats} on the client. */
    protected GodforgeModuleStats displayStats() {
        if (!isRemote()) return stats;
        GodforgeModuleStats view = new GodforgeModuleStats(stats.type);
        CompoundTag tag = syncedStats;
        view.heat = tag.getInt("heat");
        view.heatForOC = tag.getInt("heatForOC");
        view.calculatedMaxParallel = tag.getInt("parallel");
        view.speedBonus = tag.getDouble("speed");
        view.energyDiscount = tag.getDouble("discount");
        view.overclockTimeFactor = tag.getDouble("oc");
        view.processingVoltage = tag.getLong("voltage");
        view.plasmaTier = tag.getInt("plasmaTier");
        view.multiStepPlasma = tag.getBoolean("multiStep");
        view.voltageConfig = tag.getBoolean("voltageConfig");
        view.maxThreadHatchTier = tag.getInt("threadTier");
        view.magmatterCapable = tag.getBoolean("magmatter");
        return view;
    }

    /** Called by the forge after it pushed new stats. */
    @Override
    public void syncStats() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("heat", stats.heat);
        tag.putInt("heatForOC", stats.heatForOC);
        tag.putInt("parallel", stats.calculatedMaxParallel);
        tag.putDouble("speed", stats.speedBonus);
        tag.putDouble("discount", stats.energyDiscount);
        tag.putDouble("oc", stats.overclockTimeFactor);
        tag.putLong("voltage", stats.processingVoltage);
        tag.putInt("plasmaTier", stats.plasmaTier);
        tag.putBoolean("multiStep", stats.multiStepPlasma);
        tag.putBoolean("voltageConfig", stats.voltageConfig);
        tag.putInt("threadTier", stats.maxThreadHatchTier);
        tag.putBoolean("magmatter", stats.magmatterCapable);
        syncedStats = tag;
        requestSync();
    }

    @Override
    public void saveCustomPersistedData(CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
        tag.putString("powerTally", stats.powerTally.toString());
        tag.putLong("recipeTally", stats.recipeTally);
        tag.putLong("currentRecipeHeat", stats.currentRecipeHeat);
        tag.putLong("processingVoltage", stats.processingVoltage);
        tag.putBoolean("isVoltageConfigUnlocked", stats.voltageConfig);
    }

    @Override
    public void loadCustomPersistedData(CompoundTag tag) {
        super.loadCustomPersistedData(tag);
        String power = tag.getString("powerTally");
        stats.powerTally = power.isEmpty() ? BigInteger.ZERO : new BigInteger(power);
        stats.recipeTally = tag.getLong("recipeTally");
        stats.currentRecipeHeat = tag.getLong("currentRecipeHeat");
        stats.connected = connected;
        if (tag.contains("processingVoltage")) stats.processingVoltage = tag.getLong("processingVoltage");
        stats.voltageConfig = tag.getBoolean("isVoltageConfigUnlocked");
    }
}
