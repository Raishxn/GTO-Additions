package com.raishxn.gtoa.item;

import com.gregtechceu.gtceu.api.data.worldgen.bedrockfluid.BedrockFluidVeinSavedData;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.raishxn.gtoa.mining.FluidFieldLink;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import java.util.List;

public final class EntangledFluidCardItem extends Item {
    public EntangledFluidCardItem(Properties properties) { super(properties); }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        int x = context.getClickedPos().getX() >> 4, z = context.getClickedPos().getZ() >> 4;
        // This is a local survey: initialize/read native field metadata, never deplete the field.
        var entry = BedrockFluidVeinSavedData.getOrCreate(level).getFluidVeinWorldEntry(x, z);
        var definition = entry.getDefinition();
        var id = definition == null ? null : GTRegistries.BEDROCK_FLUID_DEFINITIONS.getKey(definition);
        var fluid = definition == null ? Fluids.EMPTY : definition.getStoredFluid().get();
        if (id == null || fluid == Fluids.EMPTY || entry.getFluidYield() <= 0) {
            player.displayClientMessage(Component.translatable("gtoa.oil.card.no_field"), true);
            return InteractionResult.FAIL;
        }
        new FluidFieldLink(level.dimension().location(), id, BuiltInRegistries.FLUID.getKey(fluid), x, z,
                entry.getFluidYield()).write(context.getItemInHand());
        player.displayClientMessage(Component.translatable("gtoa.oil.card.bound", id.toString(), entry.getFluidYield()), true);
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var card = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(card);
        if (!level.isClientSide && card.hasTag()) {
            card.getTag().remove(FluidFieldLink.KEY);
            player.displayClientMessage(Component.translatable("gtoa.entangled.card.cleared"), true);
        }
        return InteractionResultHolder.sidedSuccess(card, level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gtoa.oil.card.tooltip"));
        var link = FluidFieldLink.read(stack);
        if (link != null) tooltip.add(Component.translatable("gtoa.oil.card.link", link.field().toString(),
                link.fluid().toString(), link.dimension().toString(), link.chunkX(), link.chunkZ(), link.yield()));
    }
}
