package dev.example.aun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AunLoadProfileScreen extends Screen {

    private static final int MAX_VISIBLE = 5;
    private final Screen parent;
    private List<String> profileFiles = new ArrayList<>();
    private String selectedProfile = null;
    private int scrollOffset = 0;
    private Button loadButton;

    public AunLoadProfileScreen(Screen parent) {
        super(Component.literal("Load Profile"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.profileFiles = AunConfigScreen.listLoadableProfileFiles();
        if (this.selectedProfile == null && !this.profileFiles.isEmpty()) {
            this.selectedProfile = this.profileFiles.get(0);
        }

        int centerX = this.width / 2;
        int listTop = this.height / 2 - 50;
        int colWidth = 240;

        int maxScroll = Math.max(0, this.profileFiles.size() - MAX_VISIBLE);
        if (this.scrollOffset > maxScroll) this.scrollOffset = maxScroll;
        if (this.scrollOffset < 0) this.scrollOffset = 0;

        int visibleCount = Math.min(MAX_VISIBLE, this.profileFiles.size());
        for (int i = 0; i < visibleCount; i++) {
            final String fileName = this.profileFiles.get(this.scrollOffset + i);
            boolean isSelected = fileName.equals(this.selectedProfile);
            String label = isSelected ? "> " + fileName + " <" : fileName;

            this.addRenderableWidget(Button.builder(Component.literal(label), btn -> {
                this.selectedProfile = fileName;
                this.init();
            }).bounds(centerX - colWidth / 2, listTop + i * 22, colWidth, 20).build());
        }

        if (this.profileFiles.size() > MAX_VISIBLE) {
            this.addRenderableWidget(Button.builder(Component.literal("▲"), btn -> {
                if (this.scrollOffset > 0) {
                    this.scrollOffset--;
                    this.init();
                }
            }).bounds(centerX + colWidth / 2 + 6, listTop, 20, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("▼"), btn -> {
                if (this.scrollOffset < maxScroll) {
                    this.scrollOffset++;
                    this.init();
                }
            }).bounds(centerX + colWidth / 2 + 6, listTop + (MAX_VISIBLE - 1) * 22, 20, 20).build());
        }

        int bottomY = this.height / 2 + 84;
        boolean cz = AunConfigScreen.isCzechSelected();

        this.addRenderableWidget(Button.builder(Component.literal(cz ? "Zrušit" : "Cancel"), btn -> {
            Minecraft.getInstance().setScreenAndShow(this.parent);
        }).bounds(centerX - 116, bottomY, 110, 20).build());

        this.loadButton = Button.builder(Component.literal(cz ? "Načíst" : "Load"), btn -> {
            if (this.selectedProfile != null && !this.selectedProfile.isBlank()) {
                Path file = AunConfigScreen.getLoadableProfilesDir().resolve(this.selectedProfile);
                AunConfigScreen.loadProfileFromFile(file, this.selectedProfile, this.parent);
            }
        }).bounds(centerX + 6, bottomY, 110, 20).build();
        this.loadButton.active = (this.selectedProfile != null && !this.selectedProfile.isBlank());
        this.addRenderableWidget(this.loadButton);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.profileFiles.size() > MAX_VISIBLE) {
            int maxScroll = Math.max(0, this.profileFiles.size() - MAX_VISIBLE);
            int delta = scrollY > 0 ? -1 : (scrollY < 0 ? 1 : 0);
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
        int boxH = 200;
        int boxX = centerX - boxW / 2;
        int boxY = centerY - boxH / 2;

        AunConfigScreen.drawRect(graphics, boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, 0xFFFFFFFF);
        AunConfigScreen.drawRect(graphics, boxX, boxY, boxX + boxW, boxY + boxH, 0xFF181818);

        super.extractRenderState(graphics, mouseX, mouseY, a);

        boolean cz = AunConfigScreen.isCzechSelected();
        String titleText = cz ? "Vyberte profil k načtení" : "Select Profile to Load";
        int titleX = centerX - this.font.width(titleText) / 2;
        int titleY = boxY + 12;
        AunConfigScreen.drawCenteredTitle(graphics, this.font, titleText, titleX, titleY);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(this.parent);
    }
}
// END OF FILE
