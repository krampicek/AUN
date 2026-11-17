package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import dev.example.aun.client.AunConfigScreen;
import dev.example.aun.client.AunExportProfileScreen;
import dev.example.aun.client.AunLanguageScreen;
import dev.example.aun.client.AunLoadProfileScreen;
import dev.example.aun.client.AunWorldFavorites;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
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

import java.util.Locale;

@Mixin({AbstractContainerScreen.class, ChatScreen.class, AbstractSelectionList.class, AbstractWidget.class})
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
                String txt = btn.getMessage().getString().trim().toUpperCase(Locale.ROOT);
                if (txt.contains("LINK")) {
                    net.minecraft.client.Minecraft.getInstance().setScreenAndShow(new AunLoadProfileScreen(cur));
                    cir.setReturnValue(true);
                    return;
                }
                if (txt.contains("EXPORT")) {
                    net.minecraft.client.Minecraft.getInstance().setScreenAndShow(new AunExportProfileScreen(cur));
                    cir.setReturnValue(true);
                    return;
                }
                if (txt.contains("CHOOSE_LANGUAGE") || txt.contains("ZVOLIT JAZYK") || txt.contains("LANGUAGE")) {
                    net.minecraft.client.Minecraft.getInstance().setScreenAndShow(new AunLanguageScreen(cur));
                    cir.setReturnValue(true);
                    return;
                }
                if (txt.contains("SAVE (KEEP OPEN)")) {
                    AunConfigScreen.saveWithoutClosing(cur);
                    btn.setMessage(Component.literal("§aSaved!"));
                    cir.setReturnValue(true);
                    return;
                }
                if (txt.contains("BROWSE PROFILES") || txt.contains("BROWSE")) {
                    AunConfigScreen.openFolderInOS(AunConfigScreen.getProfilesBaseDir());
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
