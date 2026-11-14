package dev.example.aun.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Shared by "reach behind item frames" and "reach behind signs". */
final class BehindHelper {

    private BehindHelper() {}

    /**
     * Opens the container at {@code behind} as if the player had clicked it.
     * Returns PASS when nothing openable is there (so vanilla behaviour continues).
     * The client only reports SUCCESS so the click gets sent to the server.
     */
    static InteractionResult open(Player player, Level level, BlockPos behind, Direction face) {
        BlockState state = level.getBlockState(behind);
        if (state.isAir() || state.getMenuProvider(level, behind) == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(behind), face, behind, false);
        InteractionResult result = state.useWithoutItem(level, player, hit);
        return result.consumesAction() ? result : InteractionResult.PASS;
    }
}
