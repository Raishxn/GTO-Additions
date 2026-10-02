package com.raishxn.gtoa.machine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.blockentity.ITickSubscription;
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableEnergyContainer;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.AABB;

/** GTLCore Primitive Magic Energy behavior adapted to the dev9 holder and lifecycle. */
public final class MagicGeneratorMachine extends TieredEnergyMachine implements com.gregtechceu.gtceu.api.machine.feature.IInteractedMachine {
    private TickableSubscription generation;
    public MagicGeneratorMachine(MetaMachineBlockEntity holder, int tier) { super(holder, tier); }
    @Override public void onLoad() {
        super.onLoad();
        if (!isRemote()) generation = subscribeServerTick(generation, this::generate, 20);
    }
    @Override public void onUnload() {
        generation = ITickSubscription.unsubscribe(generation);
        super.onUnload();
    }
    private void generate() {
        if (getLevel() != null && !getLevel().getEntitiesOfClass(EndCrystal.class,
                new AABB(getPos().above()), crystal -> crystal.isAlive() && crystal.blockPosition().equals(getPos().above())).isEmpty()) {
            energyContainer.addEnergy(GTValues.V[getTier()] * 256L);
        }
    }
    @Override public net.minecraft.world.InteractionResult onUse(net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        var stack = player.getItemInHand(hand);
        if (!stack.is(net.minecraft.world.item.Items.END_CRYSTAL) || hit.getDirection() != net.minecraft.core.Direction.UP)
            return net.minecraft.world.InteractionResult.PASS;
        var above = pos.above();
        if (!level.isEmptyBlock(above) || !level.isEmptyBlock(above.above())
                || !level.getEntities(null, new AABB(above).expandTowards(0, 1, 0)).isEmpty())
            return net.minecraft.world.InteractionResult.FAIL;
        if (!level.isClientSide()) {
            var crystal = new EndCrystal(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
            crystal.setShowBottom(false);
            if (!level.addFreshEntity(crystal)) return net.minecraft.world.InteractionResult.FAIL;
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.ENTITY_PLACE, above);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide());
    }
    @Override protected NotifiableEnergyContainer createEnergyContainer(Object... args) {
        long voltage = GTValues.V[getTier()];
        return NotifiableEnergyContainer.emitterContainer(this, voltage * 512L, voltage, 16L);
    }
    @Override protected boolean isEnergyEmitter() { return true; }
    @Override protected long getMaxInputOutputAmperage() { return 16L; }
}
