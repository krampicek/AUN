package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Double doors: after a player opens or closes a door by hand, the neighbouring door of the
 * same pair (same facing, opposite hinge) follows. Doors that are already in the same state
 * are left alone, so this can never flip-flop.
 */
@Mixin(DoorBlock.class)
public abstract class DoorBlockMixin {

    @Inject(
        method = "useWithoutItem(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;"
            + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;"
            + "Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
        at = @At("RETURN"),
        require = 0
    )
    private void aun$doubleDoor(BlockState state, Level level, BlockPos pos, Player player,
                                BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (level.isClientSide()
            || !AunConfigManager.get().doubleDoors
            || !cir.getReturnValue().consumesAction()) {
            return;
        }
        BlockState now = level.getBlockState(pos);
        if (!(now.getBlock() instanceof DoorBlock)) {
            return;
        }
        BlockPos lower = now.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.below();
        BlockState lowerState = level.getBlockState(lower);
        if (!(lowerState.getBlock() instanceof DoorBlock)) {
            return;
        }
        boolean open = lowerState.getValue(DoorBlock.OPEN);
        Direction facing = lowerState.getValue(DoorBlock.FACING);
        DoorHingeSide hinge = lowerState.getValue(DoorBlock.HINGE);

        for (Direction side : new Direction[] {facing.getClockWise(), facing.getCounterClockWise()}) {
            BlockPos otherPos = lower.relative(side);
            BlockState other = level.getBlockState(otherPos);
            if (other.getBlock() instanceof DoorBlock door
                && other.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER
                && other.getValue(DoorBlock.FACING) == facing
                && other.getValue(DoorBlock.HINGE) != hinge
                && other.getValue(DoorBlock.OPEN) != open) {
                door.setOpen(player, level, other, otherPos, open);
                return;
            }
        }
    }
}
