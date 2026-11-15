package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true, require = 0)
    private void aun$applyZoom(
            Camera camera,
            float partialTick,
            boolean useFovSetting,
            CallbackInfoReturnable<Float> cir) {
        if (AunClientFeatures.isZooming()) {
            cir.setReturnValue(cir.getReturnValue() * 0.25F);
        }
    }
}
