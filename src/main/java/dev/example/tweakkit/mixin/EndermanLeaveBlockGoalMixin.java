package dev.example.tweakkit.mixin;

import dev.example.tweakkit.config.TweakConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Feature 1 (placement half): Endermen can't drop/move blocks either. */
@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanLeaveBlockGoal")
public abstract class EndermanLeaveBlockGoalMixin {

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void tweakkit$preventPlacement(CallbackInfoReturnable<Boolean> cir) {
        if (TweakConfigManager.get().antiEndermanPickup) {
            cir.setReturnValue(false);
        }
    }
}
