package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.world.entity.ai.goal.BreakDoorGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BreakDoorGoal.class)
public abstract class BreakDoorGoalMixin {

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void aun$disableDoorBreakStart(CallbackInfoReturnable<Boolean> cir) {
        if (AunConfigManager.get().noZombieDoorBreak) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    private void aun$disableDoorBreakContinue(CallbackInfoReturnable<Boolean> cir) {
        if (AunConfigManager.get().noZombieDoorBreak) {
            cir.setReturnValue(false);
        }
    }
}
