package dev.example.aun.mixin;

import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "getFluidInCamera", at = @At("RETURN"), cancellable = true, require = 0)
    private void aun$clearFluidFog(CallbackInfoReturnable cir) {
        Object val = cir.getReturnValue();
        if (val instanceof Enum<?> e) {
            String name = e.name();
            AunConfig cfg = AunConfigManager.get();
            if ((name.equals("WATER") && cfg.clearWater)
                    || (name.equals("LAVA") && cfg.clearLava)
                    || (name.equals("POWDER_SNOW") && cfg.clearPowderSnow)) {
                cir.setReturnValue(Enum.valueOf((Class<Enum>) e.getClass(), "NONE"));
            }
        }
    }
}
