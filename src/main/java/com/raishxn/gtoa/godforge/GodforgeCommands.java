package com.raishxn.gtoa.godforge;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;

/** GTNA testing helper: {@code /gtoa godforge shards <amount>} on the looked-at (or nearest) Forge of Gods. */
public final class GodforgeCommands {

    private GodforgeCommands() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("gtoa")
                .then(Commands.literal("godforge")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("shards")
                                .then(Commands.argument("amount", IntegerArgumentType.integer())
                                        .executes(context -> shards(context.getSource(),
                                                IntegerArgumentType.getInteger(context, "amount")))))));
    }

    private static int shards(CommandSourceStack source, int amount) {
        var player = source.getPlayer();
        if (player == null) return 0;
        var level = player.serverLevel();
        ForgeOfGodsMachine target = null;
        var hit = player.pick(64, 1f, false);
        if (hit instanceof net.minecraft.world.phys.BlockHitResult bhr &&
                MetaMachine.getMachine(level, bhr.getBlockPos()) instanceof ForgeOfGodsMachine fog) {
            target = fog;
        }
        if (target == null) {
            double best = Double.MAX_VALUE;
            BlockPos center = player.blockPosition();
            int r = 8;
            for (int cx = (center.getX() >> 4) - r; cx <= (center.getX() >> 4) + r; cx++) {
                for (int cz = (center.getZ() >> 4) - r; cz <= (center.getZ() >> 4) + r; cz++) {
                    var chunk = level.getChunkSource().getChunkNow(cx, cz);
                    if (chunk == null) continue;
                    for (var be : chunk.getBlockEntities().values()) {
                        if (be instanceof MetaMachineBlockEntity mbe && mbe.getMetaMachine() instanceof ForgeOfGodsMachine fog) {
                            double d = be.getBlockPos().distSqr(center);
                            if (d < best) {
                                best = d;
                                target = fog;
                            }
                        }
                    }
                }
            }
        }
        if (target == null) {
            source.sendFailure(Component.literal("No Forge of Gods controller found nearby."));
            return 0;
        }
        int total = target.addGravitonShards(amount);
        source.sendSuccess(() -> Component.literal("Forge of Gods graviton shards: " + total), true);
        return total;
    }
}
