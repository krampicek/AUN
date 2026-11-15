package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {

    @Inject(method = "guiHeight", at = @At("RETURN"), cancellable = true, require = 0)
    private void aun$raiseHotbarByTwoPixels(CallbackInfoReturnable<Integer> cir) {
        if (AunClientFeatures.inGuiHudRender && AunConfigManager.get().raisedHotbar) {
            cir.setReturnValue(cir.getReturnValue() - 2);
        }
    }
}
