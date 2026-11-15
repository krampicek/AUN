package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {

    @Shadow private long fadeOutStart;

    @Inject(method = "tick()V", at = @At("HEAD"), require = 0)
    private void aun$instantFinishFadeOut(CallbackInfo ci) {
        if (AunConfigManager.get().fadelessLoading && this.fadeOutStart > 1L) {
            this.fadeOutStart = 1L;
        }
    }
}
// END OF FILE
