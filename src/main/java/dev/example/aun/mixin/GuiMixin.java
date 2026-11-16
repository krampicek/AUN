package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import dev.example.aun.client.AunWorldFavorites;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {

    @Inject(
        method = "render(Lnet/minecraft/client/DeltaTracker;ZZ)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void aun$onHudRenderStart(DeltaTracker deltaTracker, boolean b1, boolean b2, CallbackInfo ci) {
        AunClientFeatures.updateZoomFrameAndScrollLock();
        AunWorldFavorites.renderedLocatorBarThisFrame = false;
        if (AunClientFeatures.shouldHideHudOnly()) {
            ci.cancel();
            return;
        }
        AunClientFeatures.inGuiHudRender = true;
    }

    @Inject(
        method = "render(Lnet/minecraft/client/DeltaTracker;ZZ)V",
        at = @At("RETURN"),
        require = 0
    )
    private void aun$onHudRenderEnd(DeltaTracker deltaTracker, boolean b1, boolean b2, CallbackInfo ci) {
        AunClientFeatures.inGuiHudRender = false;
    }

    @Inject(
        method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void aun$onExtractHudStart(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        AunClientFeatures.updateZoomFrameAndScrollLock();
        AunWorldFavorites.renderedLocatorBarThisFrame = false;
        if (AunClientFeatures.shouldHideHudOnly()) {
            ci.cancel();
            return;
        }
        AunClientFeatures.inGuiHudRender = true;
    }

    @Inject(
        method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V",
        at = @At("RETURN"),
        require = 0
    )
    private void aun$onExtractHudEnd(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (AunConfigManager.get().playerHud.locatorBarCombination && !AunWorldFavorites.renderedLocatorBarThisFrame) {
            AunWorldFavorites.renderPlayerHeadsOnLocatorBar(graphics);
        }
        AunClientFeatures.inGuiHudRender = false;
    }

    @ModifyConstant(method = "*", constant = @Constant(intValue = 15), require = 0)
    private int aun$centerCrosshair(int original) {
        if (!AunConfigManager.get().playerHud.centralCrosshair) return original;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getWindow() != null && (mc.getWindow().getGuiScaledWidth() & 1) == 0) {
            return 14;
        }
        return original;
    }
}
// END OF FILE
