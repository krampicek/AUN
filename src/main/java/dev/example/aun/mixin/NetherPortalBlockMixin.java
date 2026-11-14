package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Piglin portal fix: a nether portal's random tick only exists to spawn zombified piglins. */
@Pseudo
@Mixin(targets = "net.minecraft.world.level.block.NetherPortalBlock")
public abstract class NetherPortalBlockMixin {

    @Inject(
        method = "randomTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;"
            + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void aun$noPortalPiglins(BlockState state, ServerLevel level, BlockPos pos,
                                     RandomSource random, CallbackInfo ci) {
        if (AunConfigManager.get().piglinPortalFix) {
            ci.cancel();
        }
    }
}
