package com.raishxn.gtoa.item;

import com.gregtechceu.gtceu.api.block.OreBlock;
import com.gregtechceu.gtceu.integration.map.cache.server.ServerCache;
import com.raishxn.gtoa.mining.RawOrePool;
import com.raishxn.gtoa.mining.VeinLink;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import java.util.Comparator;
import java.util.List;

public final class EntangledVeinCardItem extends Item {
    public EntangledVeinCardItem(Properties properties) { super(properties); }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof OreBlock)) return InteractionResult.PASS;
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        var position = context.getClickedPos();
        var material = RawOrePool.material(level.getBlockState(position));
        ServerCache.instance.maybeInitWorld(level);
        var vein = ServerCache.instance.getNearbyVeins(level.dimension(), position, 128).stream()
                .filter(candidate -> candidate.definition().veinGenerator().getAllMaterials().contains(material))
                .min(Comparator.comparingDouble(candidate -> candidate.center().distSqr(position))).orElse(null);
        if (vein == null) {
            player.displayClientMessage(Component.translatable("gtoa.entangled.card.no_deposit"), true);
            return InteractionResult.FAIL;
        }
        if (RawOrePool.from(vein.definition()).isEmpty()) {
            player.displayClientMessage(Component.translatable("gtoa.entangled.card.no_raw"), true);
            return InteractionResult.FAIL;
        }
        new VeinLink(level.dimension().location(), vein.id(), vein.center()).write(context.getItemInHand());
        player.displayClientMessage(Component.translatable("gtoa.entangled.card.bound", vein.id().toString()), true);
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var card = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) return InteractionResultHolder.pass(card);
        if (!level.isClientSide && card.hasTag()) {
            card.getTag().remove(VeinLink.KEY);
            player.displayClientMessage(Component.translatable("gtoa.entangled.card.cleared"), true);
        }
        return InteractionResultHolder.sidedSuccess(card, level.isClientSide);
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gtoa.entangled.card.tooltip"));
        var link = VeinLink.read(stack);
        if (link != null) {
            tooltip.add(Component.translatable("gtoa.entangled.card.link", link.vein().toString(), link.dimension().toString(),
                    link.center().getX(), link.center().getY(), link.center().getZ()));
        }
    }
}
