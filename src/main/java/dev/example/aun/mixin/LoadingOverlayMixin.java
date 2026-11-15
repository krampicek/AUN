package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private ReloadInstance reload;
    @Shadow private long fadeOutStart;

    @Inject(method = {"extractRenderState", "render", "tick"}, at = @At("TAIL"), require = 0)
    private void aun$instantCloseLoadingOverlay(CallbackInfo ci) {
        if (AunConfigManager.get().fadelessLoading && this.fadeOutStart > -1L && this.reload.isDone()) {
            this.minecraft.setOverlay(null);
        }
    }
}
