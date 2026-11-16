package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightEngine.class)
public abstract class LightEngineMixin {

    @Inject(method = "getLightEmission", at = @At("RETURN"), cancellable = true, require = 0)
    private void aun$emitDynamicHeldBlockLight(BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (!AunConfigManager.get().remainingHuds.dynamicLights || pos == null) return;
        int vanilla = cir.getReturnValue();
        if (vanilla >= 15) return;
        int dynamic = AunClientFeatures.computeDynamicLightAt(pos);
        if (dynamic > vanilla) {
            cir.setReturnValue(dynamic);
        }
    }
}
// END OF FILE
