package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.server.packs.repository.PackCompatibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PackCompatibility.class)
public abstract class PackCompatibilityMixin {

    @Inject(method = "isCompatible", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$alwaysCompatibleResourcePack(CallbackInfoReturnable<Boolean> cir) {
        if (AunConfigManager.get().noResourcePackWarning) {
            cir.setReturnValue(true);
        }
    }
}
