package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Safe redstone: water (and lava) can no longer wash away redstone wire, torches, repeaters
 * and comparators. They count as solid for fluid flow, so the fluid flows around/over them.
 * Two hooks because the game checks this in two places; whichever exists is used.
 */
@Pseudo
@Mixin(targets = "net.minecraft.world.level.material.FlowingFluid")
public abstract class FlowingFluidMixin {

    @Inject(
        method = "canHoldFluid(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;"
            + "Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/Fluid;)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private static void aun$protectRedstoneA(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid,
                                      CallbackInfoReturnable<Boolean> cir) {
        if (aun$isProtected(state)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(
        method = "canHoldAnyFluid(Lnet/minecraft/world/level/block/state/BlockState;)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private static void aun$protectRedstoneB(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (aun$isProtected(state)) {
            cir.setReturnValue(false);
        }
    }

    private static boolean aun$isProtected(BlockState state) {
        if (!AunConfigManager.get().safeRedstone) {
            return false;
        }
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return id.equals("redstone_wire") || id.equals("redstone_torch") || id.equals("redstone_wall_torch")
            || id.equals("repeater") || id.equals("comparator");
    }
}
