package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import dev.example.aun.client.AunWorldFavorites;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.multiplayer.PlayerInfo;
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
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.screen != null && mc.screen.getClass().getSimpleName().equals("SelectWorldScreen")) {
            int openParen = text.indexOf(" (");
            if (openParen > 0 && text.endsWith(")") && !text.contains("🕚")) {
                String folderId = text.substring(0, openParen).trim();
                String playtime = AunWorldFavorites.getWorldPlaytimeFormatted(folderId);
                return text + "  " + playtime;
            }
        }
        return text;
    }

    @Inject(
        method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
        at = @At("HEAD"),
        require = 0
    )
    private void aun$drawChatHeadBeforeLine(
            Font font,
            net.minecraft.util.FormattedCharSequence seq,
            int x,
            int y,
            int color,
            boolean dropShadow,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!AunConfigManager.get().chatHeads || seq == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        seq.accept((index, style, codePoint) -> {
            sb.appendCodePoint(codePoint);
            return true;
        });
        String plain = sb.toString();
        if (plain.startsWith("   ")) {
            PlayerInfo sender = AunWorldFavorites.findSenderForChatLine(plain);
            if (sender != null) {
                PlayerFaceRenderer.draw((GuiGraphicsExtractor) (Object) this, sender.getSkin(), x, y, 8);
            }
        }
    }
}
