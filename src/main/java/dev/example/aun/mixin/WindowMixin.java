package dev.example.aun.mixin;

import com.mojang.blaze3d.platform.Window;
import dev.example.aun.config.AunConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public abstract class WindowMixin {

    @Inject(method = "useBorderlessFullscreenWindow", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$forceBorderlessFullscreen(CallbackInfoReturnable<Boolean> cir) {
        if (AunConfigManager.get().borderlessFullscreen) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "isExclusiveFullscreen", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$disableExclusiveFullscreen(CallbackInfoReturnable<Boolean> cir) {
        if (AunConfigManager.get().borderlessFullscreen) {
            cir.setReturnValue(false);
        }
    }
}
