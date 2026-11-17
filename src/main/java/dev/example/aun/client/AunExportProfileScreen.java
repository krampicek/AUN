package dev.example.aun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;

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

        this.nameBox = new EditBox(this.font, centerX - boxWidth / 2, centerY - 6, boxWidth, 20, Component.literal("Profile Name"));
        this.nameBox.setMaxLength(64);
        this.nameBox.setHint(Component.literal(cz ? "Zadejte název profilu..." : "Enter profile name..."));
        this.nameBox.setValue("my_profile");
        this.nameBox.setFocused(true);
        this.addRenderableWidget(this.nameBox);
        this.setFocused(this.nameBox);

        int bottomY = centerY + 36;
        this.addRenderableWidget(Button.builder(Component.literal(cz ? "Zrušit" : "Cancel"), btn -> {
            Minecraft.getInstance().setScreenAndShow(this.parent);
        }).bounds(centerX - boxWidth / 2, bottomY, 106, 20).build());

        this.exportButton = Button.builder(Component.literal(cz ? "Exportovat" : "Export"), btn -> {
            String typedName = this.nameBox.getValue();
            AunConfigScreen.exportProfileWithCustomName(this.parent, typedName);
            btn.setMessage(Component.literal(cz ? "§aExportováno" : "§aExported"));
        }).bounds(centerX + boxWidth / 2 - 106, bottomY, 106, 20).build();
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
        int boxH = 140;
        int boxX = centerX - boxW / 2;
        int boxY = centerY - boxH / 2;

        AunConfigScreen.drawRect(graphics, boxX - 1, boxY - 1, boxX + boxW + 1, boxY + boxH + 1, 0xFFFFFFFF);
        AunConfigScreen.drawRect(graphics, boxX, boxY, boxX + boxW, boxY + boxH, 0xFF181818);

        super.extractRenderState(graphics, mouseX, mouseY, a);

        boolean cz = AunConfigScreen.isCzechSelected();
        String titleText = cz ? "Pojmenujte svůj profil" : "Name Your Profile";
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
