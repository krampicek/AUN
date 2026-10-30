package dev.example.tweakkit.mixin;

import dev.example.tweakkit.config.TweakConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Feature 1 (pickup half). Mojang name of the private inner goal: EnderMan$EndermanTakeBlockGoal.
 * canUse() == false means the goal never starts.
 */
@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanTakeBlockGoal")
public abstract class EndermanTakeBlockGoalMixin {

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void tweakkit$preventPickup(CallbackInfoReturnable<Boolean> cir) {
        if (TweakConfigManager.get().antiEndermanPickup) {
            cir.setReturnValue(false);
        }
    }
}
