package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import dev.example.aun.client.AunConfigScreen;
import dev.example.aun.client.AunExportProfileScreen;
import dev.example.aun.client.AunWorldFavorites;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({
    AbstractContainerScreen.class,
    ChatScreen.class,
    AbstractSelectionList.class,
    AbstractWidget.class
})
public abstract class KeyboardHandlerMixin {

    @ModifyVariable(method = "setWidth(I)V", at = @At("HEAD"), argsOnly = true, require = 0)
    private int aun$expandLoadableProfileBoxWidth(int width) {
        if (AunConfigScreen.isProfileEditBox(this)) {
            return AunConfigScreen.PROFILE_BOX_WIDTH;
        }
        return width;
    }

    @ModifyVariable(method = "setX(I)V", at = @At("HEAD"), argsOnly = true, require = 0)
    private int aun$shiftLoadableProfileBoxLeft(int x) {
        if (AunConfigScreen.isProfileEditBox(this)) {
            return x - 72;
        }
        return x;
    }

    @Inject(method = "setY(I)V", at = @At("TAIL"), require = 0)
    private void aun$onWidgetSetY(int y, CallbackInfo ci) {
        AunConfigScreen.onWidgetSetY(this, y);
    }

    @Inject(
        method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void aun$onSubScreenOrWidgetMouseClicked(
            MouseButtonEvent event,
            boolean doubleClick,
            CallbackInfoReturnable<Boolean> cir) {
        Object self = this;

        if (self instanceof Screen screen) {
            if (AunWorldFavorites.onScreenMouseClicked(screen, event)) {
                cir.setReturnValue(true);
                return;
            }
        }

        Screen cur = AunClientFeatures.currentScreen;
        if (AunConfigScreen.isClothConfigScreen(cur) && event.button() == 0) {
            if (self instanceof Button btn) {
                String txt = btn.getMessage().getString().trim();
                if (txt.equals("EXPORT") || txt.equals("EXPORT_PROFILE")) {
                    Minecraft.getInstance().setScreenAndShow(new AunExportProfileScreen(cur));
                    cir.setReturnValue(true);
                    return;
                }
            }
            if (self instanceof AbstractSelectionList<?>) {
                if (AunConfigScreen.handleCustomMouseClick(cur, event.x(), event.y(), event.button())) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Inject(
        method = "mouseScrolled(DDDD)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void aun$onListMouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY,
            CallbackInfoReturnable<Boolean> cir) {
        Screen cur = AunClientFeatures.currentScreen;
        if (AunConfigScreen.onClothScreenMouseScrolled(cur, mouseX, mouseY, scrollY)) {
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
