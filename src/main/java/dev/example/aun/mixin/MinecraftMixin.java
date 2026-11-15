package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow public LocalPlayer player;
    @Shadow private int rightClickDelay;

    @Inject(method = "setScreenAndShow", at = @At("HEAD"), require = 0)
    private void aun$trackActiveScreen(Screen screen, CallbackInfo ci) {
        AunClientFeatures.currentScreen = screen;
    }

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void aun$accurateBlockPlacement(CallbackInfo ci) {
        Minecraft self = (Minecraft) (Object) this;
        if (this.rightClickDelay > 0 && AunClientFeatures.shouldAllowAccurateFastPlace(self)) {
            this.rightClickDelay = 0;
        }
    }
}
// END OF FILE
