package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.screen != null) {
            try {
                var m = mc.screen.getClass().getMethod("aun$handleSubScreenMouseClick", MouseButtonEvent.class);
                if (Boolean.TRUE.equals(m.invoke(mc.screen, event))) {
                    cir.setReturnValue(true);
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
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
        // J: Block Ctrl+B Narrator keybind completely
        if (AunConfigManager.get().disableNarrator && event.key() == 66 && event.hasControlDown()) {
            cir.setReturnValue(true);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.screen != null) {
            try {
                var m = mc.screen.getClass().getMethod("aun$handleSubScreenKeyPressed", KeyEvent.class);
                if (Boolean.TRUE.equals(m.invoke(mc.screen, event))) {
                    cir.setReturnValue(true);
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }
}
