package dev.example.tweakkit.mixin;

import dev.example.tweakkit.config.TweakConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Feature 1 (placement half): Endermen can't drop/move blocks either. */
@Pseudo
@Mixin(targets = {
    "net.minecraft.world.entity.monster.EnderMan$EndermanLeaveBlockGoal",
    "net.minecraft.world.entity.monster.enderman.EnderMan$EndermanLeaveBlockGoal"
})
public abstract class EndermanLeaveBlockGoalMixin {

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true, require = 0)
    private void tweakkit$preventPlacement(CallbackInfoReturnable<Boolean> cir) {
        if (TweakConfigManager.get().antiEndermanPickup) {
            cir.setReturnValue(false);
        }
    }
}
