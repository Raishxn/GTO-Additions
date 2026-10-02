package com.raishxn.gtoa.item;

import com.raishxn.gtoa.GTOAMachines;
import com.raishxn.gtoa.MachineRegistrationState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import java.util.List;

/** Supplies one complete furnace's materials without modifying the world. */
public final class PrimitiveFurnaceKitItem extends Item {
    public PrimitiveFurnaceKitItem(Properties properties) { super(properties); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack kit = player.getItemInHand(hand);
        if (!level.isClientSide) {
            MachineRegistrationState.requireRegistered();
            give(player, new ItemStack(Blocks.STONE, 23));
            give(player, GTOAMachines.PRIMITIVE_STONE_FURNACE.asStack());
            give(player, GTOAMachines.ULV_EXTENDED_INPUT_BUS.asStack());
            give(player, GTOAMachines.ULV_EXTENDED_OUTPUT_BUS.asStack());
            // Keep the held slot occupied while granting items: the use result must not overwrite
            // materials that Inventory.add could otherwise insert into the just-emptied hand slot.
            if (!player.getAbilities().instabuild) kit.shrink(1);
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }
        return InteractionResultHolder.sidedSuccess(kit, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) return InteractionResult.PASS;
        return use(context.getLevel(), context.getPlayer(), context.getHand()).getResult();
    }

    private static void give(Player player, ItemStack stack) {
        player.getInventory().add(stack);
        // Inventory.add mutates the stack: only the uninserted remainder is dropped.
        if (!stack.isEmpty()) player.drop(stack, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gtoa.furnace_kit.tooltip"));
    }
}
