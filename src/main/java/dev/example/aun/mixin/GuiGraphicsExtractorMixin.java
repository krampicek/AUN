package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import dev.example.aun.client.AunWorldFavorites;
import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin {

    @Inject(method = "guiHeight", at = @At("RETURN"), cancellable = true, require = 0)
    private void aun$raiseHotbarByTwoPixels(CallbackInfoReturnable<Integer> cir) {
        AunConfig cfg = AunConfigManager.get();
        if ((cfg.general == null || cfg.general.modEnabled)
                && AunClientFeatures.currentScreen == null
                && cfg.playerHud.raisedHotbar) {
            cir.setReturnValue(cir.getReturnValue() - 4);
        }
    }

    @ModifyVariable(
        method = {"text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V", "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)I"},
        at = @At("HEAD"),
        argsOnly = true,
        require = 0
    )
    private String aun$appendWorldPlaytimeNextToDate(String text) {
        if (text == null || !AunConfigManager.get().worldSettings.worldPlaytimeDisplay) return text;
        if (AunClientFeatures.currentScreen != null
                && AunClientFeatures.currentScreen.getClass().getSimpleName().equals("SelectWorldScreen")) {
            int openParen = text.indexOf(" (");
            if (openParen > 0 && text.endsWith(")") && !text.contains("🕚")) {
                String folderId = text.substring(0, openParen).trim();
                return text + "  " + AunWorldFavorites.getWorldPlaytimeFormatted(folderId);
            }
        }
        return text;
    }

    @Inject(
        method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
        at = @At("HEAD"),
        require = 0
    )
    private void aun$onTextSeq6(Font font, FormattedCharSequence seq, int x, int y, int color, boolean shadow, CallbackInfo ci) {
        aun$handleFormattedSeq(font, seq, x, y);
    }

    @Inject(
        method = "textWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIII)V",
        at = @At("HEAD"),
        require = 0
    )
    private void aun$onTextBackdrop(Font font, FormattedCharSequence seq, int x, int y, int width, int color, CallbackInfo ci) {
        aun$handleFormattedSeq(font, seq, x, y);
    }

    private void aun$handleFormattedSeq(Font font, FormattedCharSequence seq, int x, int y) {
        GuiGraphicsExtractor self = (GuiGraphicsExtractor) (Object) this;
        if (seq != null) {
            StringBuilder sb = new StringBuilder();
            seq.accept((idx, style, cp) -> { sb.appendCodePoint(cp); return true; });
            String plain = sb.toString();
            AunWorldFavorites.onChatLineRendered(self, font, plain, x, y);
            if (AunConfigManager.get().playerHud.chatHeads && (plain.startsWith("   ") || plain.startsWith("<"))) {
                java.util.UUID sender = AunWorldFavorites.findSenderUuidForChatLine(plain);
                if (sender != null) {
                    AunWorldFavorites.drawPlayerHead8x8(self, sender, x, y);
                }
            }
        }
        if (AunConfigManager.get().entities.horseInfo
                && AunClientFeatures.currentScreen instanceof AbstractContainerScreen<?> hs
                && AunWorldFavorites.isHorseInventoryScreen(hs)
                && x == 8 && y == 6) {
            AunWorldFavorites.drawHorseStatsInEmptyBox(self, font);
        }
    }
}
// END OF FILE
