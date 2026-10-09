package com.raishxn.gtoa.godforge;

import com.gtocore.common.data.GTOBlocks;
import com.gto.registrate.util.entry.BlockEntry;
import com.raishxn.gtoa.GTOAdditions;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;

/**
 * Forge of Gods blocks. GTO already registers the GTNH Godforge casings (GTNA uses copies of the same GTO textures),
 * so they are reused; only the Harmonic Phonon Transmission Conduit of the modules is added here.
 */
public final class GodforgeBlocks {

    private GodforgeBlocks() {}

    public static final BlockEntry<Block> SINGULARITY_SHIELDING_CASING = GTOBlocks.SINGULARITY_REINFORCED_STELLAR_SHIELDING_CASING;
    public static final BlockEntry<Block> GUIDANCE_CASING = GTOBlocks.CELESTIAL_MATTER_GUIDANCE_CASING;
    public static final BlockEntry<Block> BOUNDLESS_STRUCTURE_CASING = GTOBlocks.BOUNDLESS_GRAVITATIONALLY_SEVERED_STRUCTURE_CASING;
    public static final BlockEntry<Block> MAGNETIC_CONFINEMENT_CASING = GTOBlocks.TRANSCENDENTALLY_AMPLIFIED_MAGNETIC_CONFINEMENT_CASING;
    public static final BlockEntry<Block> STELLAR_ENERGY_SIPHON_CASING = GTOBlocks.STELLAR_ENERGY_SIPHON_CASING;
    public static final BlockEntry<Block> REMOTE_GRAVITON_FLOW_MODULATOR = GTOBlocks.REMOTE_GRAVITON_FLOW_MODULATOR;
    public static final BlockEntry<Block> MEDIAL_GRAVITON_FLOW_MODULATOR = GTOBlocks.MEDIAL_GRAVITON_FLOW_MODULATOR;
    public static final BlockEntry<Block> CENTRAL_GRAVITON_FLOW_MODULATOR = GTOBlocks.CENTRAL_GRAVITON_FLOW_MODULATOR;
    public static final BlockEntry<Block> GRAVITATIONAL_LENS = GTOBlocks.SPATIALLY_TRANSCENDENT_GRAVITATIONAL_LENS_BLOCK;
    /** GTNH Hypogen coil: the Smelting Module core. */
    public static final BlockEntry<com.gregtechceu.gtceu.common.block.CoilBlock> HYPOGEN_COIL = GTOBlocks.HYPOGEN_COIL_BLOCK;

    /** Module structure core of the Molten, Plasma and Exotic modules. */
    public static final BlockEntry<Block> HARMONIC_PHONON_TRANSMISSION_CONDUIT = GTOAdditions.REGISTRATE
            .block("harmonic_phonon_transmission_conduit", Block::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(net.minecraft.world.level.material.MapColor.METAL)
                    .strength(5, 6).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .isValidSpawn((state, level, pos, entity) -> false))
            .blockstate((ctx, prov) -> prov.simpleBlock(ctx.get(), prov.models().cubeAll(ctx.getName(),
                    GTOAdditions.id("block/casings/godforge/harmonic_phonon_transmission_conduit"))))
            .lang("Harmonic Phonon Transmission Conduit")
            .defaultLoot()
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, com.gregtechceu.gtceu.data.recipe.CustomTags.MINEABLE_WITH_WRENCH)
            .item().build().register();

    public static void init() {}
}
