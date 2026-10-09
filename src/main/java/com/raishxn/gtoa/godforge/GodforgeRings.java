package com.raishxn.gtoa.godforge;

import com.gregtechceu.gtceu.api.pattern.BlockPattern;
import com.gregtechceu.gtceu.api.pattern.FactoryBlockPattern;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;

import com.lowdragmc.lowdraglib.utils.BlockInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.google.common.base.Suppliers;
import com.raishxn.gtoa.godforge.GodforgeBlocks;
import com.raishxn.gtoa.godforge.GodforgePatterns;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;

import java.util.Map;
import java.util.function.Supplier;

import static com.gregtechceu.gtceu.api.pattern.Predicates.any;

/**
 * Ring pieces of the Forge of Gods. GTNH builds and removes the rings with {@code buildPiece} while the star renderer
 * is
 * active ({@code buildFirstRing}/{@code destroyFirstRing} and friends); here the same .mbs files are walked once with
 * recording predicates to get the world position of every ring block for the controller's orientation.
 */
public final class GodforgeRings {

    public static final com.gto.datasynclib.datastream.DataComponentKey<Map<BlockPos, Character>> RING_BLOCKS_KEY = com.gto.datasynclib.datastream.DataComponentKey
            .createNoCodec("gtoa_godforge_ring_blocks");
    public static final String RING_CHARS = "BCDEGHIK";
    /** Cells of each ring in the forge structure, set when the structure is built. */
    public static final int[] RING_CELLS = new int[3];
    private static final char RING_SYMBOL_BASE = 0xE000;

    private static final Supplier<BlockPattern>[] COLLECTORS = createCollectors();

    private GodforgeRings() {}

    /** Structure symbol of a cell of ring {@code ring} (1-3) holding {@code symbol}. */
    public static char ringSymbol(int ring, char symbol) {
        return (char) (RING_SYMBOL_BASE + ring * 0x100 + symbol);
    }

    /** Ring (1-3) of a {@link #ringSymbol} cell, 0 for any other symbol. */
    public static int ringOf(char cell) {
        int ring = (cell - RING_SYMBOL_BASE) >> 8;
        return cell >= RING_SYMBOL_BASE && ring >= 1 && ring <= 3 ? ring : 0;
    }

    @SuppressWarnings("unchecked")
    private static Supplier<BlockPattern>[] createCollectors() {
        Supplier<BlockPattern>[] collectors = new Supplier[3];
        String[] names = { "god_forge_ring_1", "god_forge_ring_2", "god_forge_ring_3" };
        for (int i = 0; i < 3; i++) {
            String name = names[i];
            collectors[i] = Suppliers.memoize(() -> {
                var symbols = new java.util.HashMap<Character, TraceabilityPredicate>();
                // GTO finds the root's anchor by its ControllerPredicate, so '~' must stay one.
                symbols.put('~', com.gregtechceu.gtceu.api.pattern.Predicates.controller(GodforgeMachines.FORGE_OF_GODS));
                for (char c : RING_CHARS.toCharArray()) symbols.put(c, recorder(c));
                return GodforgePatterns.structure(name, symbols);
            });
        }
        return collectors;
    }

    private static TraceabilityPredicate recorder(char symbol) {
        return new TraceabilityPredicate(state -> {
            Map<BlockPos, Character> blocks = state.getMatchContext().getOrCreate(RING_BLOCKS_KEY,
                    Object2ObjectLinkedOpenHashMap::new);
            blocks.put(state.pos.immutable(), symbol);
            return true;
        }, () -> BlockInfo.fromBlockState(blockFor(symbol).defaultBlockState()), () -> new Block[] { blockFor(symbol) }) {

            /** Ring blocks are claimed by the formed forge; a test-only cell skips GTO's shared-block check. */
            @Override
            public boolean testOnly() {
                return true;
            }
        };
    }

    public static Block blockFor(char symbol) {
        return switch (symbol) {
            case 'B' -> GodforgeBlocks.SINGULARITY_SHIELDING_CASING.get();
            case 'C' -> GodforgeBlocks.GUIDANCE_CASING.get();
            case 'D' -> GodforgeBlocks.BOUNDLESS_STRUCTURE_CASING.get();
            case 'E' -> GodforgeBlocks.MAGNETIC_CONFINEMENT_CASING.get();
            case 'G' -> GodforgeBlocks.REMOTE_GRAVITON_FLOW_MODULATOR.get();
            case 'H' -> GodforgeBlocks.GRAVITATIONAL_LENS.get();
            case 'I' -> GodforgeBlocks.CENTRAL_GRAVITON_FLOW_MODULATOR.get();
            case 'K' -> GodforgeBlocks.MEDIAL_GRAVITON_FLOW_MODULATOR.get();
            default -> Blocks.AIR;
        };
    }

    /** World position and pattern symbol of every block of ring {@code ring} (1-3). */
    public static Map<BlockPos, Character> positions(com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController owner,
                                                     Level level, BlockPos controller, Direction front, Direction up,
                                                     boolean flipped, int ring) {
        MultiblockState state = new MultiblockState(owner, level, controller);
        COLLECTORS[ring - 1].get().checkPatternAt(state, controller, front, up, flipped, false);
        Map<BlockPos, Character> blocks = state.getMatchContext().get(RING_BLOCKS_KEY);
        return blocks == null ? Map.of() : blocks;
    }

    /** {@code buildPiece(..._RING, ...)}: places the ring blocks without neighbour updates. */
    public static void build(Level level, Map<BlockPos, Character> blocks) {
        for (var entry : blocks.entrySet()) {
            if (!level.getBlockState(entry.getKey()).isAir()) continue;
            BlockState state = blockFor(entry.getValue()).defaultBlockState();
            level.setBlock(entry.getKey(), state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    /** {@code buildPiece(..._RING_AIR, ...)}: removes the ring blocks into the controller. */
    public static void destroy(Level level, Map<BlockPos, Character> blocks) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (var entry : blocks.entrySet()) {
            BlockPos pos = entry.getKey();
            if (!level.getBlockState(pos).is(blockFor(entry.getValue()))) continue;
            level.setBlock(pos, air, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }
}
