package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LanguageSelectScreen.class)
public abstract class LanguageSelectScreenMixin extends OptionsSubScreen {

    protected LanguageSelectScreenMixin(Screen lastScreen, Options options, Component title) {
        super(lastScreen, options, title);
    }

    @Inject(method = "onDone", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$fastLanguageReload(CallbackInfo ci) {
        if (!AunConfigManager.get().fastLanguageReload || this.minecraft == null) {
            return;
        }
        try {
            var listField = LanguageSelectScreen.class.getDeclaredField("languageSelectionList");
            listField.setAccessible(true);
            Object listWidget = listField.get(this);
            if (listWidget instanceof net.minecraft.client.gui.components.AbstractSelectionList<?> selList) {
                Object selected = selList.getSelected();
                if (selected != null) {
                    var codeField = selected.getClass().getDeclaredField("code");
                    codeField.setAccessible(true);
                    String code = (String) codeField.get(selected);
                    if (code != null && !code.equals(this.minecraft.getLanguageManager().getSelected())) {
                        this.options.languageCode = code;
                        this.minecraft.getLanguageManager().setSelected(code);
                        this.minecraft.getLanguageManager().onResourceManagerReload(this.minecraft.getResourceManager());
                        this.options.save();
                    }
                    this.minecraft.setScreenAndShow(this.lastScreen);
                    ci.cancel();
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
