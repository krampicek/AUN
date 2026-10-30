package dev.example.tweakkit.mixin;

import dev.example.tweakkit.util.InstaMineLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Features 4 & 5: a destroy-progress of 1.0 or more means "instant break". Client
 * (prediction) and server (authoritative) both call this, so install on both for multiplayer.
 */
@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {

    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void tweakkit$instaMine(BlockState state, Player player, BlockGetter level,
                                    BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (InstaMineLogic.shouldInstaMine(state, player)) {
            cir.setReturnValue(1.0F);
        }
    }
}
