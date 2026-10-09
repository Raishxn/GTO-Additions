package com.raishxn.gtoa.godforge;

import com.gregtechceu.gtceu.api.recipe.handler.IO;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.multiblock.PartAbility;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import com.gto.datasynclib.annotations.SaveToDisk;
import com.gto.datasynclib.annotations.SyncToClient;
import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import com.raishxn.gtoa.godforge.api.GodforgeData;
import com.raishxn.gtoa.godforge.api.GodforgeMath;
import com.raishxn.gtoa.godforge.api.GodforgeModuleStats;
import com.raishxn.gtoa.godforge.api.GodforgeUpgrade;
import com.raishxn.gtoa.godforge.api.GodforgeUpgradeStorage;
import com.raishxn.gtoa.godforge.GodforgeBlocks;
import com.raishxn.gtoa.godforge.GodforgeMaterialRefs;
import com.raishxn.gtoa.godforge.GodforgePatterns;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static com.gregtechceu.gtceu.api.pattern.Predicates.*;

/**
 * Forge of Gods controller, ported from GTNH {@code MTEForgeOfGods} (GT5-Unofficial a3e1e112). It burns stellar fuel
 * into an internal battery, unlocks upgrades with graviton shards and drives connected modules every five seconds.
 */
public class ForgeOfGodsMachine extends WorkableMultiblockMachine
                                implements com.gregtechceu.gtceu.api.machine.feature.IMachineLife,
                                com.gregtechceu.gtceu.api.machine.feature.IUIMachine {

    public static final com.gto.datasynclib.datastream.DataComponentKey<List<BlockPos>> MODULES_KEY = com.gto.datasynclib.datastream.DataComponentKey
            .createNoCodec("gtoa_godforge_modules");
    private static final int UPDATE_PERIOD = 100;

    private final GodforgeData data = new GodforgeData();
    private final List<BlockPos> modulePositions = new ArrayList<>();

    @SaveToDisk
    @Nullable
    private UUID networkOwner;

    // Client-visible star state for the renderer.
    @SyncToClient
    private boolean renderActive;
    @SyncToClient
    private int syncedRingAmount = 1;
    @SyncToClient
    private int syncedStarSize = GodforgeData.DEFAULT_STAR_SIZE;
    @SyncToClient
    private int syncedRotationSpeed = GodforgeData.DEFAULT_ROTATION_SPEED;
    @SyncToClient
    private String syncedStarColor = com.raishxn.gtoa.godforge.api.GodforgeStarColor.DEFAULT
            .serialize();

    /** Full forge state for the GUI on the client (GTNH syncs the same data through its sync hypervisor). */
    @SyncToClient
    private CompoundTag syncedData = new CompoundTag();
    @SyncToClient
    private int connectedModules;
    /** {@code storedUpgradeWindowItems}: the manual insertion window of upgrade extra costs. */
    @SaveToDisk
    private final com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler upgradeWindow = new com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler(
            16);
    @Nullable
    private GodforgeData clientView;
    @Nullable
    private CompoundTag clientViewTag;

    @Nullable
    private TickableSubscription tickSubscription;
    private long ticker;

    public ForgeOfGodsMachine(MetaMachineBlockEntity holder) {
        super(holder);
    }

    public GodforgeData data() {
        return data;
    }

    /** The live data on the server, a copy rebuilt from the synced tag on the client. */
    public GodforgeData view() {
        if (!isRemote()) return data;
        if (clientView == null || clientViewTag != syncedData) {
            GodforgeData copy = new GodforgeData();
            GodforgeDataNbt.load(copy, syncedData);
            GodforgeMath.milestones(copy);
            GodforgeMath.inversionStatus(copy);
            clientView = copy;
            clientViewTag = syncedData;
        }
        return clientView;
    }

    public int connectedModules() {
        return connectedModules;
    }

    public com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler upgradeWindow() {
        return upgradeWindow;
    }

    public boolean isRenderActive() {
        return renderActive;
    }

    public int ringAmount() {
        return syncedRingAmount;
    }

    public int starSize() {
        return syncedStarSize;
    }

    public int rotationSpeed() {
        return syncedRotationSpeed;
    }

    public String starColor() {
        return syncedStarColor;
    }

    // ---- Pattern ----

    /** Symbol table of the controller structure (also used by the definition's {@code Structure}). */
    public static java.util.Map<Character, TraceabilityPredicate> symbols(@Nullable MultiblockMachineDefinition definition) {
        var map = new java.util.LinkedHashMap<Character, TraceabilityPredicate>();
        map.put('~', definition == null ? any() : controller(definition));
        map.put('A', blocks(GodforgeBlocks.MAGNETIC_CONFINEMENT_CASING.get())
                .or(standardHatch(PartAbility.IMPORT_ITEMS))
                .or(standardHatch(PartAbility.IMPORT_FLUIDS))
                .or(standardHatch(PartAbility.EXPORT_ITEMS)));
        map.put('B', blocks(GodforgeBlocks.SINGULARITY_SHIELDING_CASING.get()));
        map.put('C', blocks(GodforgeBlocks.GUIDANCE_CASING.get()));
        map.put('D', blocks(GodforgeBlocks.BOUNDLESS_STRUCTURE_CASING.get()));
        map.put('E', blocks(GodforgeBlocks.MAGNETIC_CONFINEMENT_CASING.get()));
        map.put('F', blocks(GodforgeBlocks.STELLAR_ENERGY_SIPHON_CASING.get()));
        map.put('G', blocks(GodforgeBlocks.REMOTE_GRAVITON_FLOW_MODULATOR.get()));
        map.put('H', blocks(GodforgeBlocks.GRAVITATIONAL_LENS.get()));
        map.put('I', blocks(GodforgeBlocks.CENTRAL_GRAVITON_FLOW_MODULATOR.get()));
        map.put('J', moduleSlot());
        map.put('K', blocks(GodforgeBlocks.MEDIAL_GRAVITON_FLOW_MODULATOR.get()));
        map.put('L', air());
        map.put(' ', any());
        return map;
    }

    public static final com.gto.datasynclib.datastream.DataComponentKey<java.util.Map<BlockPos, Character>>[] RING_KEYS = ringKeys();

    @SuppressWarnings("unchecked")
    private static com.gto.datasynclib.datastream.DataComponentKey<java.util.Map<BlockPos, Character>>[] ringKeys() {
        var keys = new com.gto.datasynclib.datastream.DataComponentKey[3];
        for (int i = 0; i < 3; i++) {
            keys[i] = com.gto.datasynclib.datastream.DataComponentKey.createNoCodec("gtoa_godforge_ring_" + (i + 1));
        }
        return keys;
    }

    /**
     * A cell of ring {@code ring} ({@code symbol} is its block). While the star is out the ring must be built; while
     * the lit star holds the ring the cell may also be air (blocks put back there are taken in again by the next
     * structure check). Matched cells are recorded, giving the exact world position of every ring block.
     */
    public static TraceabilityPredicate ringCell(int ring, char symbol) {
        var block = GodforgeRings.blockFor(symbol);
        return new TraceabilityPredicate(state -> {
            boolean absorbed = state.controller instanceof ForgeOfGodsMachine forge && forge.data.renderActive &&
                    ring <= Math.max(1, forge.data.ringAmount);
            boolean matched = state.getBlockState().is(block) || absorbed && state.getBlockState().isAir();
            if (matched) {
                state.getMatchContext().getOrCreate(RING_KEYS[ring - 1],
                        it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap::new)
                        .put(state.pos.immutable(), symbol);
            }
            return matched;
        }, () -> BlockInfo.fromBlock(block), () -> new net.minecraft.world.level.block.Block[] { block });
    }

    /** Ring blocks recorded by the last successful structure check (null until then). */
    @SuppressWarnings("unchecked")
    private final java.util.Map<BlockPos, Character>[] ringBlocks = new java.util.Map[3];
    /** Whether each ring was complete (as blocks, or as air inside the star) in the last structure check. */
    private final boolean[] ringPresent = new boolean[3];
    /**
     * Ring blocks are being placed/removed. GTO's {@code requestCheck} on a formed machine drops the structure and
     * re-forms it asynchronously, so the forge's own ring changes (which keep the structure valid) must not trigger it.
     */
    private boolean mutatingRings;

    private static com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern withBlocks(
                                                                                    com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern pattern,
                                                                                    @Nullable MultiblockMachineDefinition definition) {
        symbols(definition).forEach(pattern::where);
        return pattern;
    }

    /**
     * GTNH {@code checkOneInputBus/Hatch/OutputBus}: exactly one of each. The preview and the Nexus Terminal only
     * offer GTCEu's regular buses and hatches (other parts sharing the ability, such as tesseracts, still match).
     */
    private static TraceabilityPredicate standardHatch(PartAbility ability) {
        TraceabilityPredicate predicate = abilities(ability).setExactLimit(1);
        for (var simple : predicate.limited) {
            var all = simple.candidates;
            if (all == null) continue;
            simple.candidates = () -> java.util.Arrays.stream(all.get())
                    .filter(block -> net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getNamespace()
                            .equals("gtceu"))
                    .toArray(net.minecraft.world.level.block.Block[]::new);
        }
        return predicate;
    }

    /** A module controller or the shielding casing; module positions are collected for the controller. */
    private static TraceabilityPredicate moduleSlot() {
        return new TraceabilityPredicate(state -> {
            if (state.getBlockState().is(GodforgeBlocks.SINGULARITY_SHIELDING_CASING.get())) return true;
            if (MetaMachine.getMachine(state.world, state.pos) instanceof IGodforgeModule) {
                List<BlockPos> modules = state.getMatchContext().getOrCreate(MODULES_KEY, ArrayList::new);
                modules.add(state.pos.immutable());
                return true;
            }
            return false;
        }, () -> BlockInfo.fromBlock(GodforgeBlocks.SINGULARITY_SHIELDING_CASING.get()),
                () -> new net.minecraft.world.level.block.Block[] { GodforgeBlocks.SINGULARITY_SHIELDING_CASING.get() });
    }

    @Override
    public void requestCheck() {
        if (!mutatingRings) super.requestCheck();
    }

    /** Runs a ring change without dropping the structure. */
    private void mutateRings(Runnable change) {
        boolean outer = !mutatingRings;
        mutatingRings = true;
        try {
            change.run();
        } finally {
            if (outer) mutatingRings = false;
        }
    }

    // ---- Lifecycle ----

    @Override
    public void onMachinePlaced(@Nullable LivingEntity player, ItemStack stack) {
        if (player != null) networkOwner = player.getUUID();
        // A fresh controller never holds rings: a lit state carried over would let ring cells match air.
        data.renderActive = false;
    }

    @Override
    public void onStructureFormed() {
        super.onStructureFormed();
        modulePositions.clear();
        List<BlockPos> found = getMultiblockState().getMatchContext().get(MODULES_KEY);
        if (found != null) modulePositions.addAll(found);
        var context = getMultiblockState().getMatchContext();
        for (int i = 0; i < 3; i++) {
            java.util.Map<BlockPos, Character> blocks = context.get(RING_KEYS[i]);
            ringPresent[i] = blocks != null && blocks.size() == GodforgeRings.RING_CELLS[i];
            if (ringPresent[i]) ringBlocks[i] = new java.util.HashMap<>(blocks);
        }
        updateRings();
        tickSubscription = subscribeServerTick(tickSubscription, this::godforgeTick);
    }

    @Override
    public void onStructureInvalid() {
        super.onStructureInvalid();
        var state = getMultiblockState();
        com.mojang.logging.LogUtils.getLogger().info("Forge of Gods at {} invalid: {} at {} (star lit {})", getPos(),
                state.error == null ? "?" : state.error.getErrorInfo().getString(), state.pos, data.renderActive);
        forEachModule(IGodforgeModule::disconnect);
        modulePositions.clear();
        if (tickSubscription != null) {
            tickSubscription.unsubscribe();
            tickSubscription = null;
        }
    }

    /** Ring section of {@code checkMachine}: rings 2 and 3 count with CD and END and join an active star. */
    private void updateRings() {
        if (!(getLevel() instanceof ServerLevel)) return;
        boolean cd = data.isUpgradeActive(GodforgeUpgrade.CD);
        boolean end = cd && data.isUpgradeActive(GodforgeUpgrade.END);
        if (!data.renderActive) {
            data.ringAmount = cd && ringPresent[1] ? (end && ringPresent[2] ? 3 : 2) : 1;
        } else {
            // An upgrade that was lost puts its ring back as blocks.
            if (data.ringAmount == 3 && !end) {
                buildRing(3);
                data.ringAmount = 2;
            }
            if (data.ringAmount == 2 && !cd) {
                buildRing(2);
                data.ringAmount = 1;
            }
            // A freshly built ring next to a lit star is taken in.
            if (cd && data.ringAmount == 1 && ringPresent[1]) {
                data.ringAmount = 2;
                mutateRings(() -> GodforgeRings.destroy(getLevel(), ring(2)));
            }
            if (end && data.ringAmount == 2 && ringPresent[2]) {
                data.ringAmount = 3;
                mutateRings(() -> GodforgeRings.destroy(getLevel(), ring(3)));
            }
            // The star holds rings 1..ringAmount: ring blocks placed back there are taken in again.
            if (!data.rendererDisabled) {
                mutateRings(() -> {
                    for (int ring = 1; ring <= data.ringAmount; ring++) GodforgeRings.destroy(getLevel(), ring(ring));
                });
            }
        }
        syncVisuals();
    }

    /** Ring blocks of the last structure check, or (before any) walked from the ring pattern. */
    private java.util.Map<BlockPos, Character> ring(int ring) {
        var known = ringBlocks[ring - 1];
        if (known != null) return known;
        return GodforgeRings.positions(this, getLevel(), getPos(), getFrontFacing(), getUpwardsFacing(), isFlipped(), ring);
    }

    /** Rebuilds a ring that was taken into the controller. */
    private void buildRing(int ring) {
        mutateRings(() -> GodforgeRings.build(getLevel(), ring(ring)));
    }

    /** {@code createRenderer}: the star appears and the rings are taken into the controller. */
    private void createRenderer() {
        if (!(getLevel() instanceof ServerLevel level) || data.renderActive) return;
        data.renderActive = true;
        mutateRings(() -> {
            for (int ring = 1; ring <= data.ringAmount; ring++) GodforgeRings.destroy(level, ring(ring));
        });
        syncVisuals();
        onChanged();
    }

    /** {@code destroyRenderer}: the star goes out and the rings are placed back. */
    private void destroyRenderer() {
        if (!(getLevel() instanceof ServerLevel level) || !data.renderActive) return;
        data.renderActive = false;
        mutateRings(() -> {
            for (int ring = 1; ring <= data.ringAmount; ring++) GodforgeRings.build(level, ring(ring));
        });
        syncVisuals();
        onChanged();
    }

    @Override
    public void onMachineRemoved() {
        destroyRenderer();
        clearInventory(upgradeWindow);
    }

    /** Rotation is locked while the rings are inside the controller, as in {@code isRotationChangeAllowed}. */
    @Override
    public boolean isFacingValid(net.minecraft.core.Direction facing) {
        if (data.renderActive && facing != getFrontFacing()) return false;
        return super.isFacingValid(facing);
    }

    @Override
    protected net.minecraft.world.InteractionResult onScrewdriverClick(net.minecraft.world.entity.player.Player player,
                                                                       net.minecraft.world.InteractionHand hand,
                                                                       net.minecraft.core.Direction gridSide,
                                                                       net.minecraft.world.phys.BlockHitResult hit) {
        if (isRemote()) return net.minecraft.world.InteractionResult.SUCCESS;
        toggleRenderer();
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(data.rendererDisabled ?
                "gtoa.godforge.animations.disabled" : "gtoa.godforge.animations.enabled"));
        return net.minecraft.world.InteractionResult.CONSUME;
    }

    private void godforgeTick() {
        if (!isFormed() || !(getLevel() instanceof ServerLevel)) return;
        if (++ticker % UPDATE_PERIOD != 0) return;
        if (networkOwner == null) networkOwner = getOwnerUUID();

        // An empty battery always puts the star out and its rings back.
        if (data.renderActive && data.internalBattery == 0) destroyRenderer();
        absorbStartupFuelAndShards();
        if (data.internalBattery != 0 && !data.renderActive && !data.rendererDisabled) createRenderer();
        if (data.internalBattery != 0) drainFuel();

        List<GodforgeModuleStats> moduleStats = new ArrayList<>();
        forEachModule(module -> moduleStats.add(module.godforgeStats()));
        GodforgeMath.compositionLevel(data, moduleStats);
        GodforgeMath.milestones(data);
        GodforgeMath.inversionStatus(data);
        GodforgeMath.gravitonShardAmount(data);
        if (data.isUpgradeActive(GodforgeUpgrade.END) && data.gravitonShardEjection) ejectGravitonShards();

        int maxModules = GodforgeMath.maxModuleCount(data);
        boolean canRun = data.internalBattery > 0 && modulePositions.size() <= maxModules;
        forEachModule(module -> {
            GodforgeModuleStats stats = module.godforgeStats();
            if (canRun && GodforgeMath.allowConnection(stats, data)) {
                module.setNetworkOwner(networkOwner);
                module.connect();
                GodforgeMath.updateModule(stats, data);
                module.syncStats();
                if (GodforgeMath.factorChangeDuringRecipe(stats)) module.disconnect();
            } else {
                module.disconnect();
            }
        });
        syncVisuals();
        onChanged();
    }

    private void forEachModule(java.util.function.Consumer<IGodforgeModule> action) {
        if (getLevel() == null) return;
        for (BlockPos pos : modulePositions) {
            if (MetaMachine.getMachine(getLevel(), pos) instanceof IGodforgeModule module) action.accept(module);
        }
    }

    private static final boolean GODFORGE_DEBUG = true;

    private void credit(boolean lit, int amount) {
        if (amount <= 0) return;
        if (!lit) {
            data.stellarFuelAmount += amount;
        } else {
            data.gravitonShardsAvailable += amount;
            data.gravitonShardsSpent -= amount;
        }
    }

    /** Stellar Fuel before the star is lit; graviton shards afterwards once END is unlocked. */
    private void absorbStartupFuelAndShards() {
        boolean lit = data.internalBattery != 0;
        if (lit && !data.isUpgradeActive(GodforgeUpgrade.END)) return;
        // 1) recipe handler units (as recipes see the parts), 2) Forge item capability of every part as fallback.
        net.minecraft.world.item.Item wanted = lit ? GodforgeItems.GRAVITON_SHARD.get() : null;
        java.util.function.Predicate<ItemStack> matches = stack -> !stack.isEmpty() &&
                (lit ? stack.is(wanted) : isStellarFuel(stack));
        int before = lit ? data.gravitonShardsAvailable : data.stellarFuelAmount;
        int units = 0, handlers = 0;
        for (var unit : inputUnits) {
            units++;
            for (var handler : unit.itemHandlers) {
                handlers++;
                if (!(handler instanceof com.gregtechceu.gtceu.api.recipe.handler.IItemRecipeHandler items)) continue;
                java.util.Map<net.minecraft.world.item.Item, long[]> found = new java.util.HashMap<>();
                items.fastForEachItems((stack, amount) -> {
                    if (GODFORGE_DEBUG && amount > 0) com.mojang.logging.LogUtils.getLogger().info("Forge of Gods bus item {} x{} fuel={}",
                            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()), amount, isStellarFuel(stack));
                    if (amount > 0 && matches.test(stack)) found.computeIfAbsent(stack.getItem(), k -> new long[1])[0] += amount;
                });
                for (var entry : found.entrySet()) {
                    int amount = (int) Math.min(entry.getValue()[0], 1_000_000);
                    var request = new ArrayList<com.gregtechceu.gtceu.api.recipe.content.Content<com.gregtechceu.gtceu.api.recipe.ingredient.ItemIngredient>>();
                    request.add(new com.gregtechceu.gtceu.api.recipe.content.Content<>(
                            com.gregtechceu.gtceu.api.recipe.ingredient.ItemIngredient.of(entry.getKey(), amount), amount));
                    items.handleRecipeItem(IO.IN, com.gregtechceu.gtceu.api.recipe.GTRecipe.EMPTY, request, false);
                    credit(lit, (int) (amount - request.stream().mapToLong(c -> c.amount).sum()));
                }
            }
        }
        if ((lit ? data.gravitonShardsAvailable : data.stellarFuelAmount) == before) {
            for (var part : getParts()) {
                var be = part.self().holder;
                if (be == null) continue;
                be.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER).ifPresent(inv -> {
                    for (int slot = 0; slot < inv.getSlots(); slot++) {
                        ItemStack stack = inv.getStackInSlot(slot);
                        if (!matches.test(stack)) continue;
                        ItemStack taken = inv.extractItem(slot, stack.getCount(), false);
                        credit(lit, taken.getCount());
                    }
                });
            }
        }
        if (GODFORGE_DEBUG) com.mojang.logging.LogUtils.getLogger().info(
                "Forge of Gods fuel scan: units={} itemHandlers={} parts={} fuel={} shards={}", units, handlers,
                getParts().length, data.stellarFuelAmount, data.gravitonShardsAvailable);
        if (!lit) {
            data.neededStartupFuel = GodforgeMath.startupFuelConsumption(data);
            if (data.stellarFuelAmount >= data.neededStartupFuel) {
                data.stellarFuelAmount -= data.neededStartupFuel;
                data.increaseBattery(data.neededStartupFuel);
                if (!data.rendererDisabled) createRenderer();
            }
        }
    }

    /**
     * GTNH uses Avaritia's Stellar Fuel and falls back to a Neutronium block without Avaritia; GTNA uses the
     * fallback.
     */
    /** Any Neutronium storage block (GTCEu's or another mod's tagged one). */
    public static boolean isStellarFuel(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var entry = com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper.getMaterialEntry(stack.getItem());
        if (entry != null && entry.material() == GTMaterials.Neutronium &&
                entry.tagPrefix() == com.gregtechceu.gtceu.api.data.tag.TagPrefix.block) return true;
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return id.equals("neutronium_block") || id.equals("block_of_neutronium") ||
                stack.is(net.minecraft.tags.ItemTags.create(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/neutronium")));
    }


    public static ItemStack stellarFuel() {
        return com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper.get(
                com.gregtechceu.gtceu.api.data.tag.TagPrefix.block, GTMaterials.Neutronium);
    }

    private Fluid selectedFuel() {
        return switch (data.selectedFuelType) {
            case 0 -> GodforgeMaterialRefs.DimensionallyTranscendentResidue().getFluid();
            case 1 -> GodforgeMaterialRefs.RawStarMatter().getFluid();
            default -> GodforgeMaterialRefs.MagnetohydrodynamicallyConstrainedStarMatter().getFluid();
        };
    }

    private void drainFuel() {
        GodforgeMath.clampFuelFactor(data);
        int factor = data.fuelConsumptionFactor;
        data.fuelConsumption = (long) Math.max(GodforgeMath.fuelConsumption(data) * 5 *
                (data.batteryCharging ? 2 : 1), 1);
        if (data.fuelConsumption >= Integer.MAX_VALUE) {
            reduceBattery(factor);
            return;
        }
        // Through the recipe handler path, so creative/infinite hatches behave as in recipes.
        Fluid fuel = selectedFuel();
        var left = fuelRequest(fuel);
        for (var unit : inputUnits) {
            unit.handleRecipeFluid(IO.IN, com.gregtechceu.gtceu.api.recipe.GTRecipe.EMPTY, left, true);
            if (left.isEmpty()) break;
        }
        if (left.isEmpty()) {
            left = fuelRequest(fuel);
            for (var unit : inputUnits) {
                unit.handleRecipeFluid(IO.IN, com.gregtechceu.gtceu.api.recipe.GTRecipe.EMPTY, left, false);
                if (left.isEmpty()) break;
            }
            data.totalFuelConsumed += factor;
            if (data.batteryCharging) data.increaseBattery(factor);
            return;
        }
        reduceBattery(factor);
    }

    private List<com.gregtechceu.gtceu.api.recipe.content.Content<com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient>> fuelRequest(
                                                                                                                                     Fluid fuel) {
        var list = new ArrayList<com.gregtechceu.gtceu.api.recipe.content.Content<com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient>>();
        list.add(new com.gregtechceu.gtceu.api.recipe.content.Content<>(
                com.gregtechceu.gtceu.api.recipe.ingredient.FluidIngredient.of(fuel, data.fuelConsumption), data.fuelConsumption));
        return list;
    }

    /** Item storages of every part with the given direction (any bus type, including addon ones). */
    private List<com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler> itemHandlers(IO io) {
        List<com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler> result = new ArrayList<>();
        for (var part : getParts()) {
            for (var trait : part.self().getTraits()) {
                if (trait instanceof com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler items &&
                        (items.handlerIO == io || items.handlerIO == IO.BOTH))
                    result.add(items.storage);
            }
        }
        return result;
    }

    private List<com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank> fluidHandlers(IO io) {
        List<com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank> result = new ArrayList<>();
        for (var part : getParts()) {
            for (var trait : part.self().getTraits()) {
                if (trait instanceof com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank tank &&
                        (tank.handlerIO == io || tank.handlerIO == IO.BOTH))
                    result.add(tank);
            }
        }
        return result;
    }

    private void reduceBattery(long amount) {
        if (data.reduceBattery(amount)) {
            forEachModule(IGodforgeModule::disconnect);
            destroyRenderer();
        }
    }

    private void ejectGravitonShards() {
        for (var storage : itemHandlers(IO.OUT)) {
            int available = data.gravitonShardsAvailable;
            ItemStack shard = GodforgeItems.gravitonShard(1);
            int ejected = 0;
            for (int slot = 0; slot < storage.getSlots() && ejected < available; slot++) {
                ItemStack batch = shard.copyWithCount(Math.min(shard.getMaxStackSize(), available - ejected));
                ItemStack left = storage.insertItem(slot, batch, false);
                ejected += batch.getCount() - left.getCount();
            }
            data.gravitonShardsAvailable -= ejected;
            data.gravitonShardsSpent += ejected;
            return;
        }
    }

    // ---- Upgrades (called by the GUI) ----

    public boolean unlockUpgrade(GodforgeUpgrade upgrade) {
        boolean changed = data.unlockUpgrade(upgrade);
        if (changed && (upgrade == GodforgeUpgrade.CD || upgrade == GodforgeUpgrade.END)) updateRings();
        if (changed) changed();
        return changed;
    }

    public boolean respecUpgrade(GodforgeUpgrade upgrade) {
        boolean changed = data.respecUpgrade(upgrade);
        if (changed && (upgrade == GodforgeUpgrade.CD || upgrade == GodforgeUpgrade.END)) updateRings();
        if (changed) changed();
        return changed;
    }

    /** Screwdriver toggle of the star animation, as in the original. */
    public void toggleRenderer() {
        if (data.rendererDisabled) {
            data.rendererDisabled = false;
        } else {
            data.rendererDisabled = true;
            destroyRenderer();
        }
        syncVisuals();
        onChanged();
    }

    // ---- GUI ----

    @Override
    public com.lowdragmc.lowdraglib.gui.modular.ModularUI createUI(net.minecraft.world.entity.player.Player player) {
        return com.raishxn.gtoa.godforge.gui.ForgeOfGodsUI.createUI(this, player);
    }

    // ---- GUI actions (server side) ----

    public void payUpgradeCost(GodforgeUpgrade upgrade) {
        List<GodforgeUpgradeStorage.Payment> payments = new ArrayList<>();
        for (int slot = 0; slot < upgradeWindow.getSlots(); slot++) {
            final int index = slot;
            ItemStack stack = upgradeWindow.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            payments.add(new GodforgeUpgradeStorage.Payment() {

                @Override
                public String item() {
                    return id;
                }

                @Override
                public int amount() {
                    return upgradeWindow.getStackInSlot(index).getCount();
                }

                @Override
                public void shrink(int by) {
                    upgradeWindow.extractItem(index, by, false);
                }
            });
        }
        data.upgrades.payCost(upgrade, payments);
        changed();
    }

    public void setFuelType(int type) {
        data.selectedFuelType = Math.max(0, Math.min(2, type));
        GodforgeMath.clampFuelFactor(data);
        changed();
    }

    public void setFuelFactor(int factor) {
        data.fuelConsumptionFactor = Math.max(1, Math.min(factor, GodforgeMath.maxFuelFactor(data)));
        changed();
    }

    public void toggleBatteryCharging() {
        data.batteryCharging = !data.batteryCharging;
        changed();
    }

    /** {@code MAX_BATTERY_CHARGE}: configurable once REC is unlocked. */
    public void setMaxBatteryCharge(long charge) {
        if (!data.isUpgradeActive(GodforgeUpgrade.REC)) return;
        data.maxBatteryCharge = Math.max(1, charge);
        if (data.internalBattery > data.maxBatteryCharge) data.internalBattery = data.maxBatteryCharge;
        changed();
    }

    public void toggleShardEjection() {
        if (!data.isUpgradeActive(GodforgeUpgrade.END)) return;
        data.gravitonShardEjection = !data.gravitonShardEjection;
        changed();
    }

    public void toggleSecretUpgrade() {
        data.secretUpgrade = !data.secretUpgrade;
        changed();
    }

    /** "Refresh module connection status": re-runs the structure check. */
    public void refreshModules() {
        if (getLevel() instanceof ServerLevel && checkPatternWithLock()) onStructureFormed();
        changed();
    }

    public void setRotationSpeed(int speed) {
        data.rotationSpeed = Math.max(0, Math.min(100, speed));
        changed();
    }

    public void setStarSize(int size) {
        data.starSize = Math.max(0, Math.min(40, size));
        changed();
    }

    public void selectStarColor(String name) {
        if (data.starColors.byName(name) == null) return;
        data.selectedStarColor = name;
        changed();
    }

    /** Stores a new custom color or replaces the custom color at {@code index} (from the color editor). */
    public void saveStarColor(String serialized, int index) {
        var color = com.raishxn.gtoa.godforge.api.GodforgeStarColor.deserializeString(serialized);
        if (color == null || color.numColors() == 0) return;
        if (index >= 0 && index < data.starColors.size() && !data.starColors.byIndex(index).isPreset()) {
            String old = data.starColors.byIndex(index).name();
            data.starColors.insert(color, index);
            if (old.equals(data.selectedStarColor)) data.selectedStarColor = color.name();
        } else {
            data.starColors.store(color);
        }
        changed();
    }

    public void deleteStarColor(String name) {
        var color = data.starColors.byName(name);
        if (color == null || color.isPreset()) return;
        data.starColors.drop(color);
        if (name.equals(data.selectedStarColor)) {
            data.selectedStarColor = com.raishxn.gtoa.godforge.api.GodforgeStarColor.DEFAULT.name();
        }
        changed();
    }

    /** Debug/testing entry for {@code /gtoa godforge shards}. */
    public int addGravitonShards(int amount) {
        // Available shards are recomputed every tick as milestone total minus spent, so a grant lowers "spent".
        int granted = Math.max(-data.gravitonShardsAvailable, amount);
        data.gravitonShardsSpent -= granted;
        data.gravitonShardsAvailable += granted;
        changed();
        return data.gravitonShardsAvailable;
    }

    private void changed() {
        syncVisuals();
        onChanged();
    }

    private void syncVisuals() {
        renderActive = data.renderActive && !data.rendererDisabled;
        syncedRingAmount = data.ringAmount;
        syncedData = GodforgeDataNbt.save(data);
        int connected = 0;
        if (getLevel() != null) {
            for (BlockPos pos : modulePositions) {
                if (MetaMachine.getMachine(getLevel(), pos) instanceof IGodforgeModule module &&
                        module.godforgeStats().connected)
                    connected++;
            }
        }
        connectedModules = connected;
        syncedStarSize = data.starSize;
        syncedRotationSpeed = data.rotationSpeed;
        var color = data.starColors.byName(data.selectedStarColor);
        syncedStarColor = (color == null ? com.raishxn.gtoa.godforge.api.GodforgeStarColor.DEFAULT :
                color).serialize();
        // datasynclib only pushes @SyncToClient fields when asked.
        if (getLevel() != null && !isRemote()) requestSync();
    }

    // ---- Persistence ----

    @Override
    public void saveCustomPersistedData(CompoundTag tag, boolean forDrop) {
        super.saveCustomPersistedData(tag, forDrop);
        CompoundTag godforge = GodforgeDataNbt.save(data);
        if (forDrop) godforge.putBoolean("isRenderActive", false);
        tag.put("godforge", godforge);
    }

    @Override
    public void loadCustomPersistedData(CompoundTag tag) {
        super.loadCustomPersistedData(tag);
        GodforgeDataNbt.load(data, tag.getCompound("godforge"));
        syncVisuals();
    }
}
