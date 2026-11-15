package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {

    @ModifyConstant(method = "*", constant = @Constant(floatValue = 1000.0F), require = 0)
    private float aun$instantFadeOutFloat(float original) {
        return AunConfigManager.get().fadelessLoading ? 1.0F : original;
    }

    @ModifyConstant(method = "*", constant = @Constant(floatValue = 500.0F), require = 0)
    private float aun$instantFadeInFloat(float original) {
        return AunConfigManager.get().fadelessLoading ? 1.0F : original;
    }
}
