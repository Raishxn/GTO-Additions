package com.raishxn.gtoa;

import com.gto.registrate.util.entry.BlockEntry;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.tags.BlockTags;

public final class GTOABlocks {
    private GTOABlocks() {}
    public static final BlockEntry<Block> UNIVERSAL_FACTORY_CASING = GTOAdditions.REGISTRATE
            .block("universal_factory_casing", Block::new)
            .initialProperties(() -> Blocks.IRON_BLOCK)
            .properties(p -> p.mapColor(net.minecraft.world.level.material.MapColor.METAL)
                    .strength(5, 6).sound(SoundType.METAL).requiresCorrectToolForDrops()
                    .isValidSpawn((state, level, pos, entity) -> false))
            .lang("Universal Factory Casing")
            .tag(BlockTags.MINEABLE_WITH_PICKAXE, com.gregtechceu.gtceu.data.recipe.CustomTags.MINEABLE_WITH_WRENCH)
            .item().build().register();
}
