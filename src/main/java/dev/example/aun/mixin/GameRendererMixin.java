package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import dev.example.aun.client.FullbrightHandler;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.NarratorStatus;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OptionInstance.class)
public abstract class GameRendererMixin {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "get", at = @At("RETURN"), cancellable = true, require = 0)
    private void aun$overrideGammaAndFov(CallbackInfoReturnable cir) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.options == null) return;
        Object self = this;
        if (self == mc.options.gamma() && FullbrightHandler.isFullbrightActive()) {
            cir.setReturnValue(16.0D);
        } else if (self == mc.options.fov() && AunClientFeatures.isZooming()) {
            Object val = cir.getReturnValue();
            if (val instanceof Integer fov) {
                cir.setReturnValue(Math.max(15, fov / 4));
            }
        }
    }

    @Inject(method = "set", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$lockNarratorOff(Object value, CallbackInfo ci) {
        if (!AunConfigManager.get().hud.disableNarrator) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options != null && (Object) this == mc.options.narrator()) {
            if (value != NarratorStatus.OFF || mc.options.narrator().get() == NarratorStatus.OFF) {
                ci.cancel();
            }
        }
    }
}
// END OF FILE
