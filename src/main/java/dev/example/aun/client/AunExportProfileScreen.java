package dev.example.aun.client;

import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;

public class AunExportProfileScreen extends Screen {

    private static Method fillMethod = null;
    private static Method textMethod = null;
    private static boolean searchedDrawMethods = false;

    private final Screen parent;
    private EditBox nameBox;

    public AunExportProfileScreen(Screen parent) {
        super(Component.literal("Export Own Profile"));
        this.parent = parent;
    }

    private static boolean isCzech() {
        AunConfig cfg = AunConfigManager.getRealConfig();
        return cfg.selectedLanguage != null && cfg.selectedLanguage.equalsIgnoreCase("cs_cz");
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int boxWidth = 220;
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        AunConfig cfg = AunConfigManager.getRealConfig();
        String defaultName = (cfg.profile != null && cfg.profile.customProfilePath != null && !cfg.profile.customProfilePath.isBlank())
                ? cfg.profile.customProfilePath.replace(".json", "")
                : "my_profile";

        this.nameBox = new EditBox(this.font, centerX - boxWidth / 2, centerY - 12, boxWidth, 20, Component.literal("Profile Name"));
        this.nameBox.setMaxLength(64);
        this.nameBox.setValue(defaultName);
        this.addRenderableWidget(this.nameBox);
        this.setInitialFocus(this.nameBox);

        String cancelLabel = isCzech() ? "Zrušit" : "Cancel";
        String exportLabel = isCzech() ? "Exportovat" : "Export";
        String exportedLabel = isCzech() ? "§aExportováno" : "§aExported";

        // Cancel button on the left below the black box
        this.addRenderableWidget(Button.builder(Component.literal(cancelLabel), btn -> {
            Minecraft.getInstance().setScreenAndShow(this.parent);
        }).bounds(centerX - boxWidth / 2, centerY + 18, 106, 20).build());

        // Export button on the right below the black box (white initially, green "Exported" after click)
        this.addRenderableWidget(Button.builder(Component.literal(exportLabel), btn -> {
            String typedName = this.nameBox.getValue();
            AunConfigScreen.exportProfileWithCustomName(this.parent, typedName);
            btn.setMessage(Component.literal(exportedLabel));
        }).bounds(centerX + boxWidth / 2 - 106, centerY + 18, 106, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        drawClassicMenuFrame(graphics, this.width, this.height);
        super.extractRenderState(graphics, mouseX, mouseY, a);

        String title = isCzech() ? "Exportovat vlastní profil" : "Export Own Profile";
        String sub = isCzech() ? "Zadejte název profilu:" : "Enter profile name:";
        drawCenteredText(graphics, this.font, title, this.width / 2, 12, 0xFFFFFFFF);
        drawCenteredText(graphics, this.font, sub, this.width / 2, this.height / 2 - 28, 0xFFA0A0A0);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(this.parent);
    }

    // =========================================================================
    // CLASSIC MINECRAFT WINDOW 2: LOAD PROFILE SCREEN (OPENED BY "LINK" BUTTON)
    // =========================================================================
    public static class LoadProfileScreen extends Screen {

        private final Screen parent;
        private List<String> profileFiles;
        private String selectedProfile;
        private int scrollOffset = 0;

        public LoadProfileScreen(Screen parent) {
            super(Component.literal("Load Profile"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            this.clearWidgets();
            this.profileFiles = AunConfigScreen.listLoadableProfileFiles();
            AunConfig cfg = AunConfigManager.getRealConfig();
            if (this.selectedProfile == null) {
                this.selectedProfile = (cfg.profile != null && cfg.profile.customProfilePath != null && !cfg.profile.customProfilePath.isBlank())
                        ? cfg.profile.customProfilePath
                        : (this.profileFiles.isEmpty() ? "default_profile.json" : this.profileFiles.get(0));
            }

            int listTop = 38;
            int listBottom = this.height - 38;
            int maxVisible = Math.max(1, (listBottom - listTop) / 24);
            int maxScroll = Math.max(0, this.profileFiles.size() - maxVisible);
            if (this.scrollOffset > maxScroll) this.scrollOffset = maxScroll;
            if (this.scrollOffset < 0) this.scrollOffset = 0;

            int colWidth = 240;
            int colX = this.width / 2 - colWidth / 2;

            for (int i = 0; i < maxVisible && (i + this.scrollOffset) < this.profileFiles.size(); i++) {
                final String fileName = this.profileFiles.get(i + this.scrollOffset);
                boolean isSelected = fileName.equalsIgnoreCase(this.selectedProfile);
                String label = isSelected ? ("> " + fileName + " <") : fileName;
                int rowY = listTop + i * 24;

                this.addRenderableWidget(Button.builder(Component.literal(label), btn -> {
                    this.selectedProfile = fileName;
                    this.init();
                }).bounds(colX, rowY, colWidth, 20).build());
            }

            if (this.profileFiles.size() > maxVisible) {
                this.addRenderableWidget(Button.builder(Component.literal("▲"), b -> {
                    if (this.scrollOffset > 0) { this.scrollOffset--; this.init(); }
                }).bounds(colX + colWidth + 6, listTop, 20, 20).build());

                this.addRenderableWidget(Button.builder(Component.literal("▼"), b -> {
                    if (this.scrollOffset < maxScroll) { this.scrollOffset++; this.init(); }
                }).bounds(colX + colWidth + 6, listBottom - 20, 20, 20).build());
            }

            String cancelLabel = isCzech() ? "Zrušit" : "Cancel";
            String loadLabel = isCzech() ? "Načíst" : "Load";

            // Bottom buttons: Cancel (left) and Load (right)
            this.addRenderableWidget(Button.builder(Component.literal(cancelLabel), btn -> {
                Minecraft.getInstance().setScreenAndShow(this.parent);
            }).bounds(this.width / 2 - 104, this.height - 26, 100, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal(loadLabel), btn -> {
                if (this.selectedProfile != null && !this.selectedProfile.isBlank()) {
                    Path file = AunConfigScreen.getLoadableProfilesDir().resolve(this.selectedProfile);
                    AunConfigScreen.loadProfileFromFile(file, this.selectedProfile, this.parent);
                } else {
                    Minecraft.getInstance().setScreenAndShow(this.parent);
                }
            }).bounds(this.width / 2 + 4, this.height - 26, 100, 20).build());
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            int maxVisible = Math.max(1, (this.height - 76) / 24);
            int maxScroll = Math.max(0, this.profileFiles.size() - maxVisible);
            int delta = scrollY > 0 ? -1 : (scrollY < 0 ? 1 : 0);
            if (delta != 0) {
                int next = Math.max(0, Math.min(maxScroll, this.scrollOffset + delta));
                if (next != this.scrollOffset) {
                    this.scrollOffset = next;
                    this.init();
                    return true;
                }
            }
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            drawClassicMenuFrame(graphics, this.width, this.height);
            super.extractRenderState(graphics, mouseX, mouseY, a);
            String title = isCzech() ? "Načíst profil (aun_profiles_loadable)" : "Load Profile (aun_profiles_loadable)";
            drawCenteredText(graphics, this.font, title, this.width / 2, 12, 0xFFFFFFFF);
        }

        @Override
        public void onClose() {
            Minecraft.getInstance().setScreenAndShow(this.parent);
        }
    }

    // =========================================================================
    // CLASSIC MINECRAFT WINDOW 3: LANGUAGE SELECTOR SCREEN
    // =========================================================================
    public static class LanguageScreen extends Screen {

        private static final String[][] LANGUAGES = {
            {"en_us", "English (US)"},
            {"cs_cz", "Čeština (CZ)"}
        };

        private final Screen parent;
        private String selectedLangCode;

        public LanguageScreen(Screen parent) {
            super(Component.literal("Language"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            this.clearWidgets();
            AunConfig cfg = AunConfigManager.getRealConfig();
            if (this.selectedLangCode == null) {
                this.selectedLangCode = (cfg.selectedLanguage != null && !cfg.selectedLanguage.isBlank())
                        ? cfg.selectedLanguage
                        : "en_us";
            }

            int colWidth = 220;
            int colX = this.width / 2 - colWidth / 2;
            int startY = 52;

            for (int i = 0; i < LANGUAGES.length; i++) {
                final String code = LANGUAGES[i][0];
                final String displayName = LANGUAGES[i][1];
                boolean isSelected = code.equalsIgnoreCase(this.selectedLangCode);
                String label = isSelected ? ("> " + displayName + " <") : displayName;

                this.addRenderableWidget(Button.builder(Component.literal(label), btn -> {
                    this.selectedLangCode = code;
                    this.init();
                }).bounds(colX, startY + i * 24, colWidth, 20).build());
            }

            String cancelLabel = isCzech() ? "Zrušit" : "Cancel";
            String loadLabel = isCzech() ? "Načíst" : "Load";

            this.addRenderableWidget(Button.builder(Component.literal(cancelLabel), btn -> {
                Minecraft.getInstance().setScreenAndShow(this.parent);
            }).bounds(this.width / 2 - 104, this.height - 26, 100, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal(loadLabel), btn -> {
                AunConfigScreen.applySelectedLanguageAndReopen(this.selectedLangCode, this.parent);
            }).bounds(this.width / 2 + 4, this.height - 26, 100, 20).build());
        }

        @Override
        public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            drawClassicMenuFrame(graphics, this.width, this.height);
            super.extractRenderState(graphics, mouseX, mouseY, a);
            String title = isCzech() ? "Vyberte jazyk" : "Select Language";
            drawCenteredText(graphics, this.font, title, this.width / 2, 12, 0xFFFFFFFF);
        }

        @Override
        public void onClose() {
            Minecraft.getInstance().setScreenAndShow(this.parent);
        }
    }

    // =========================================================================
    // DRAWING HELPERS FOR CLASSIC MINECRAFT MENU WINDOWS
    // =========================================================================
    private static void ensureDrawMethods(GuiGraphicsExtractor g) {
        if (!searchedDrawMethods) {
            searchedDrawMethods = true;
            try { fillMethod = g.getClass().getMethod("fill", int.class, int.class, int.class, int.class, int.class); }
            catch (Throwable ignored) {}
            try { textMethod = g.getClass().getMethod("text", Font.class, String.class, int.class, int.class, int.class, boolean.class); }
            catch (Throwable ignored) {}
        }
    }

    private static void drawClassicMenuFrame(GuiGraphicsExtractor g, int width, int height) {
        ensureDrawMethods(g);
        if (fillMethod != null) {
            try {
                // Darkened classic menu backdrop + central list box + header/footer borders
                fillMethod.invoke(g, 0, 0, width, height, 0xC0101010);
                fillMethod.invoke(g, 0, 30, width, height - 32, 0x90000000);
                fillMethod.invoke(g, 0, 30, width, 31, 0xFF808080);
                fillMethod.invoke(g, 0, height - 33, width, height - 32, 0xFF808080);
            } catch (Throwable ignored) {}
        }
    }

    private static void drawCenteredText(GuiGraphicsExtractor g, Font font, String text, int centerX, int y, int color) {
        ensureDrawMethods(g);
        if (textMethod != null && font != null && text != null) {
            try {
                int x = centerX - font.width(text) / 2;
                textMethod.invoke(g, font, text, x, y, color, true);
            } catch (Throwable ignored) {}
        }
    }
}
// END OF FILE
