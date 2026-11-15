package dev.example.aun.mixin;

import dev.example.aun.client.AunWorldFavorites;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.LocatorBar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocatorBar.class)
public abstract class LocatorBarMixin {

    @Inject(method = "extractRenderState", at = @At("TAIL"), require = 0)
    private void aun$renderPlayerHeadsOnLocatorBar(
            GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker,
            CallbackInfo ci) {
        AunWorldFavorites.updateHeadLocatorOverlay();
    }
}
