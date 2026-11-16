package dev.example.aun.mixin;

import dev.example.aun.client.AunWorldFavorites;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.LocatorBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocatorBar.class)
public abstract class LocatorBarMixin {

    @Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$combineLocatorBarIntoXpBar(
            GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker,
            CallbackInfo ci) {
        if (AunConfigManager.get().playerHud.locatorBarCombination) {
            AunWorldFavorites.drawCombinedXpBarBackground(graphics);
            ci.cancel();
        }
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$replaceLocatorDotsWithPlayerHeads(
            GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker,
            CallbackInfo ci) {
        AunWorldFavorites.renderedLocatorBarThisFrame = true;
        if (AunConfigManager.get().playerHud.headLocator) {
            AunWorldFavorites.renderPlayerHeadsOnLocatorBar(graphics);
            ci.cancel();
        }
    }
}
// END OF FILE
