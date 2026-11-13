package dev.example.aun.mixin;

import dev.example.aun.util.InstaMineLogic;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Features 4 and 5: destroy progress of 1.0 means "instant break". */
@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {

    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void aun$instaMine(BlockState state, Player player, BlockGetter level,
                               BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (InstaMineLogic.shouldInstaMine(state, player)) {
            cir.setReturnValue(1.0F);
        }
    }
}
