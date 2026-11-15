package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import dev.example.aun.client.AunWorldFavorites;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {

    @Inject(method = "guiHeight", at = @At("RETURN"), cancellable = true, require = 0)
    private void aun$raiseHotbarByTwoPixels(CallbackInfoReturnable<Integer> cir) {
        if (AunClientFeatures.inGuiHudRender && AunConfigManager.get().raisedHotbar) {
            cir.setReturnValue(cir.getReturnValue() - 2);
        }
    }

    @ModifyVariable(
        method = {"text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V", "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"},
        at = @At("HEAD"),
        argsOnly = true,
        require = 0
    )
    private String aun$appendWorldPlaytimeNextToDate(String text) {
        if (text == null || !AunConfigManager.get().worldPlaytimeDisplay) {
            return text;
        }
        if (AunClientFeatures.currentScreen != null
                && AunClientFeatures.currentScreen.getClass().getSimpleName().equals("SelectWorldScreen")) {
            int openParen = text.indexOf(" (");
            if (openParen > 0 && text.endsWith(")") && !text.contains("🕚")) {
                String folderId = text.substring(0, openParen).trim();
                String playtime = AunWorldFavorites.getWorldPlaytimeFormatted(folderId);
                return text + "  " + playtime;
            }
        }
        return text;
    }
}
// END OF FILE
