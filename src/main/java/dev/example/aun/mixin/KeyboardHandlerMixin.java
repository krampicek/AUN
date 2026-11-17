package dev.example.aun.mixin;

import dev.example.aun.client.AunConfigScreen;
import dev.example.aun.client.AunWorldFavorites;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractContainerScreen.class, ChatScreen.class, AbstractWidget.class})
public abstract class KeyboardHandlerMixin {

    @ModifyVariable(
        method = "setMessage(Lnet/minecraft/network/chat/Component;)V",
        at = @At("HEAD"),
        argsOnly = true,
        require = 0
    )
    private Component aun$forceDisabledLabelOnCheatyButtons(Component message) {
        if (AunConfigScreen.isLockedCheatyButton(this)) {
            return Component.literal("Disabled");
        }
        return message;
    }

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
        if (!((Object) this instanceof Screen screen)) return;
        if (AunWorldFavorites.onScreenMouseClicked(screen, event)) {
            cir.setReturnValue(true);
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
        if (!((Object) this instanceof Screen screen)) return;
        if (AunConfigManager.get().remainingHuds.disableNarrator && event.key() == 66 && event.hasControlDown()) {
            cir.setReturnValue(true);
            return;
        }
        if (AunWorldFavorites.onScreenKeyPressed(screen, event)) {
            cir.setReturnValue(true);
        }
    }
}
// END OF FILE
