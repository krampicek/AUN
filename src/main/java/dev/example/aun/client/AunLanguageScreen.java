package dev.example.aun.client;

import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;

public class AunLanguageScreen extends Screen {

    private static final String[][] LANGUAGES = {
        {"en_us", "English (US)"},
        {"cs_cz", "Čeština (CZ)"}
    };

    private final Screen parent;
    private String selectedLangCode;

    public AunLanguageScreen(Screen parent) {
        super(Component.literal("Language"));
        this.parent = parent;
        AunConfig cfg = AunConfigManager.getRealConfig();
        this.selectedLangCode = (cfg.selectedLanguage != null && !cfg.selectedLanguage.isBlank())
                ? cfg.selectedLanguage
                : "en_us";
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int centerX = this.width / 2;
        int listTop = this.height / 2 - 28;
        int colWidth = 220;

        for (int i = 0; i < LANGUAGES.length; i++) {
            final String code = LANGUAGES[i][0];
            final String name = LANGUAGES[i][1];
            boolean isSelected = code.equalsIgnoreCase(this.selectedLangCode);
            String label = isSelected ? "> " + name + " <" : name;

            this.addRenderableWidget(Button.builder(Component.literal(label), btn -> {
                this.selectedLangCode = code;
                this.init();
            }).bounds(centerX - colWidth / 2, listTop + i * 26, colWidth, 20).build());
        }

        int bottomY = this.height / 2 + 54;
        boolean cz = AunConfigScreen.isCzechSelected();

        this.addRenderableWidget(Button.builder(Component.literal(cz ? "Zrušit" : "Cancel"), btn -> {
            Minecraft.getInstance().setScreenAndShow(this.parent);
        }).bounds(centerX - 110, bottomY, 104, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(cz ? "Načíst" : "Load"), btn -> {
            AunConfig cfg = AunConfigManager.getRealConfig();
            cfg.selectedLanguage = this.selectedLangCode;
            try {
                AutoConfig.getConfigHolder(AunConfig.class).save();
            } catch (Throwable ignored) {}
            AunConfigScreen.applyModLanguage(this.selectedLangCode);
            Minecraft.getInstance().setScreenAndShow(this.parent);
        }).bounds(centerX + 6, bottomY, 104, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (this.parent != null) {
            try {
                Method m = this.parent.getClass().getMethod("extractRenderState", GuiGraphicsExtractor.class, int.class, int.class, float.class);
                m.invoke(this.parent, graphics, -1, -1, a);
            } catch (Throwable ignored) {
                try {
                    Method m2 = this.parent.getClass().getMethod("render", GuiGraphicsExtractor.class, int.class, int.class, float.class);
                    m2.invoke(this.parent, graphics, -1, -1, a);
                } catch (Throwable ignored2) {}
            }
        }

        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int boxW = 280;
        int boxH = 160;
        int boxX = centerX - boxW / 2;
        int boxY = centerY - boxH / 2;

        AunConfigScreen.drawRect(graphics, boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, 0xFFFFFFFF);
        AunConfigScreen.drawRect(graphics, boxX, boxY, boxX + boxW, boxY + boxH, 0xFF181818);

        super.extractRenderState(graphics, mouseX, mouseY, a);

        boolean cz = AunConfigScreen.isCzechSelected();
        String titleText = cz ? "Vyberte jazyk" : "Select Language";
        int titleX = centerX - this.font.width(titleText) / 2;
        int titleY = boxY + 16;
        AunConfigScreen.drawCenteredTitle(graphics, this.font, titleText, titleX, titleY);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(this.parent);
    }
}
// END OF FILE
