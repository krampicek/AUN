package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TemptGoal.class)
public abstract class TemptGoalMixin {

    @Shadow private int calmDown;

    @Inject(method = "stop", at = @At("TAIL"))
    private void aun$clearCalmDownOnStop(CallbackInfo ci) {
        if (AunConfigManager.get().noAnimalTemptDelay) {
            this.calmDown = 0;
        }
    }

    @Inject(method = "canUse", at = @At("HEAD"))
    private void aun$clearCalmDownOnCheck(CallbackInfoReturnable<Boolean> cir) {
        if (AunConfigManager.get().noAnimalTemptDelay) {
            this.calmDown = 0;
        }
    }
}
