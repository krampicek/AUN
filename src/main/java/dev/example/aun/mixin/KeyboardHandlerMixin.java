package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import dev.example.aun.client.AunWorldFavorites;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractContainerScreen.class, ChatScreen.class})
public abstract class KeyboardHandlerMixin {

    @Inject(
        method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void aun$onSubScreenMouseClicked(
            MouseButtonEvent event,
            boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir) {
        if (AunWorldFavorites.onScreenMouseClicked((Screen) (Object) this, event)) {
            cir.setReturnValue(true);
        }
    }

    @ModifyVariable(
        method = "keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z",
        at = @At("HEAD"),
        argsOnly = true,
        require = 0
    )
    private KeyEvent aun$remapSprintDropToCtrlThrow(KeyEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options != null && mc.options.keyDrop.matches(event)) {
            boolean sprintHeld = AunClientFeatures.isKeyMappingPhysicallyDown(mc.options.keySprint);
            if (AunConfigManager.get().remainingHuds.ctrlDropAllSameItems && sprintHeld) {
                return new KeyEvent(event.key(), event.keycode(), event.modifiers() | 2);
            } else {
                return new KeyEvent(event.key(), event.keycode(), event.modifiers() & ~2);
            }
        }
        return event;
    }

    @Inject(
        method = "keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void aun$onSubScreenKeyPressed(
            KeyEvent event,
            CallbackInfoReturnable<Boolean> cir) {
        if (AunConfigManager.get().remainingHuds.disableNarrator && event.key() == 66 && event.hasControlDown()) {
            cir.setReturnValue(true);
            return;
        }
        if (AunWorldFavorites.onScreenKeyPressed((Screen) (Object) this, event)) {
            cir.setReturnValue(true);
        }
    }
}
// END OF FILE
