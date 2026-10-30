package dev.example.tweakkit.mixin;

import dev.example.tweakkit.config.TweakConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Feature 1 (pickup half). The class path of this private inner class moved between
 * versions, so both likely locations are listed. @Pseudo = "if the class does not
 * exist, skip quietly instead of crashing the game".
 */
@Pseudo
@Mixin(targets = {
    "net.minecraft.world.entity.monster.EnderMan$EndermanTakeBlockGoal",
    "net.minecraft.world.entity.monster.enderman.EnderMan$EndermanTakeBlockGoal"
})
public abstract class EndermanTakeBlockGoalMixin {

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true, require = 0)
    private void tweakkit$preventPickup(CallbackInfoReturnable<Boolean> cir) {
        if (TweakConfigManager.get().antiEndermanPickup) {
            cir.setReturnValue(false);
        }
    }
}
