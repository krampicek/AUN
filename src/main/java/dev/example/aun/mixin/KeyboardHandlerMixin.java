package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import dev.example.aun.client.AunConfigScreen;
import dev.example.aun.client.AunExportProfileScreen;
import dev.example.aun.client.AunLanguageScreen;
import dev.example.aun.client.AunLoadProfileScreen;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({
    AbstractContainerScreen.class,
    ChatScreen.class,
    AbstractSelectionList.class,
    AbstractWidget.class
})
public abstract class KeyboardHandlerMixin {

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

        Screen cur = Minecraft.getInstance().screen;
        if (AunConfigScreen.isClothConfigScreen(cur) && event.button() == 0) {
            if (self instanceof Button btn) {
                String txt = btn.getMessage().getString().trim().toUpperCase(java.util.Locale.ROOT);
                if (txt.contains("LINK")) {
                    Minecraft.getInstance().setScreenAndShow(new AunLoadProfileScreen(cur));
                    cir.setReturnValue(true);
                    return;
                }
                if (txt.contains("EXPORT")) {
                    Minecraft.getInstance().setScreenAndShow(new AunExportProfileScreen(cur));
                    cir.setReturnValue(true);
                    return;
                }
                if (txt.contains("CHOOSE_LANGUAGE") || txt.contains("ZVOLIT JAZYK")) {
                    Minecraft.getInstance().setScreenAndShow(new AunLanguageScreen(cur));
                    cir.setReturnValue(true);
                    return;
                }
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
