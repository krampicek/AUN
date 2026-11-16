package dev.example.aun.mixin;

import com.mojang.blaze3d.platform.Window;
import dev.example.aun.client.AunClientFeatures;
import dev.example.aun.config.AunConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Window.class)
public abstract class WindowMixin {

    @ModifyVariable(method = "setExclusiveFullscreen", at = @At("HEAD"), argsOnly = true, require = 0)
    private boolean aun$forceBorderlessFullscreen(boolean exclusive) {
        if (AunConfigManager.get().screenSettings.borderlessFullscreen) {
            return false;
        }
        return exclusive;
    }

    @Inject(method = "getGuiScaledHeight", at = @At("RETURN"), cancellable = true, require = 0)
    private void aun$raiseHotbarAboveScreenBottom(CallbackInfoReturnable<Integer> cir) {
        if (AunClientFeatures.inGuiHudRender && AunConfigManager.get().playerHud.raisedHotbar) {
            cir.setReturnValue(cir.getReturnValue() - 2);
        }
    }
}
// END OF FILE
