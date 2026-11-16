package dev.example.aun.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class AunExportProfileScreen extends Screen {

    private final Screen parent;
    private EditBox nameBox;
    private Button exportButton;

    public AunExportProfileScreen(Screen parent) {
        super(Component.literal("Export Profile"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.clearWidgets();

        int boxWidth = 220;
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        boolean cz = AunConfigScreen.isCzechSelected();

        this.nameBox = new EditBox(this.font, centerX - boxWidth / 2, centerY - 14, boxWidth, 20, Component.literal("Profile Name"));
        this.nameBox.setMaxLength(64);
        this.nameBox.setHint(Component.literal(cz ? "Zadejte název profilu..." : "Enter profile name..."));
        this.nameBox.setValue("my_profile");
        this.nameBox.setFocused(true);
        this.addRenderableWidget(this.nameBox);
        this.setFocused(this.nameBox);

        // Cancel button on the left below the black box (white text)
        this.addRenderableWidget(Button.builder(Component.literal(cz ? "Zrušit" : "Cancel"), btn -> {
            AunConfigScreen.reopenClothScreen(this.parent);
        }).bounds(centerX - boxWidth / 2, centerY + 14, 106, 20).build());

        // Export button on the right below the black box (white initially, green "Exported" after click)
        this.exportButton = Button.builder(Component.literal(cz ? "Exportovat" : "Export"), btn -> {
            String typedName = this.nameBox.getValue();
            AunConfigScreen.exportProfileWithCustomName(this.parent, typedName);
            btn.setMessage(Component.literal(cz ? "§aExportováno" : "§aExported"));
        }).bounds(centerX + boxWidth / 2 - 106, centerY + 14, 106, 20).build();
        this.addRenderableWidget(this.exportButton);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.nameBox != null && this.nameBox.isMouseOver(event.x(), event.y())) {
            if (event.button() == 1) {
                this.nameBox.setValue("");
            }
            this.nameBox.setFocused(true);
            this.setFocused(this.nameBox);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == 256) {
            this.onClose();
            return true;
        }
        if (this.nameBox != null && this.nameBox.isFocused()) {
            if (event.key() == 259) {
                String cur = this.nameBox.getValue();
                if (!cur.isEmpty()) {
                    this.nameBox.setValue(cur.substring(0, cur.length() - 1));
                }
                return true;
            }
            if (this.nameBox.keyPressed(event)) {
                return true;
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (this.nameBox != null && this.nameBox.isFocused()) {
            String ch = event.codepointAsString();
            if (ch != null && !ch.isEmpty() && ch.charAt(0) >= 32) {
                String cur = this.nameBox.getValue();
                if (cur.length() < 64) {
                    this.nameBox.setValue(cur + ch);
                }
                return true;
            }
        }
        return super.charTyped(event);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        boolean cz = AunConfigScreen.isCzechSelected();
        String titleText = cz ? "Pojmenujte svůj profil" : "Name Your Profile";
        int titleX = this.width / 2 - this.font.width(titleText) / 2;
        int titleY = this.height / 2 - 34;
        AunConfigScreen.drawCenteredTitle(graphics, this.font, titleText, titleX, titleY);
    }

    @Override
    public void onClose() {
        AunConfigScreen.reopenClothScreen(this.parent);
    }
}
// END OF FILE
