package dev.example.aun.client;

import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

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
        int listTop = this.height / 2 - 36;
        int colWidth = 220;

        for (int i = 0; i < LANGUAGES.length; i++) {
            final String code = LANGUAGES[i][0];
            final String name = LANGUAGES[i][1];
            boolean isSelected = code.equalsIgnoreCase(this.selectedLangCode);
            String label = isSelected ? "> " + name + " <" : name;

            this.addRenderableWidget(Button.builder(Component.literal(label), btn -> {
                this.selectedLangCode = code;
                this.init();
            }).bounds(centerX - colWidth / 2, listTop + i * 24, colWidth, 20).build());
        }

        int bottomY = this.height - 32;
        boolean cz = "cs_cz".equalsIgnoreCase(this.selectedLangCode);

        this.addRenderableWidget(Button.builder(Component.literal(cz ? "Zrušit" : "Cancel"), btn -> {
            AunConfigScreen.reopenClothScreen(this.parent);
        }).bounds(centerX - 108, bottomY, 104, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(cz ? "Načíst" : "Load"), btn -> {
            AunConfig cfg = AunConfigManager.getRealConfig();
            cfg.selectedLanguage = this.selectedLangCode;
            try {
                AutoConfig.getConfigHolder(AunConfig.class).save();
            } catch (Throwable ignored) {}
            AunConfigScreen.applyModLanguage(this.selectedLangCode);
            AunConfigScreen.reopenClothScreen(this.parent);
        }).bounds(centerX + 4, bottomY, 104, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        boolean cz = "cs_cz".equalsIgnoreCase(this.selectedLangCode);
        String titleText = cz ? "Vyberte jazyk" : "Select Language";
        int titleX = this.width / 2 - this.font.width(titleText) / 2;
        int titleY = this.height / 2 - 60;
        AunConfigScreen.drawCenteredTitle(graphics, this.font, titleText, titleX, titleY);
    }

    @Override
    public void onClose() {
        AunConfigScreen.reopenClothScreen(this.parent);
    }
}
// END OF FILE
