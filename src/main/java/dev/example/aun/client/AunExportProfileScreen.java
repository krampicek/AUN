package dev.example.aun.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
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

        this.nameBox = new EditBox(this.font, centerX - boxWidth / 2, centerY - 14, boxWidth, 20, Component.literal("Profile Name"));
        this.nameBox.setMaxLength(64);
        this.nameBox.setHint(Component.literal("Enter profile name..."));
        this.nameBox.setValue("my_profile");
        this.addRenderableWidget(this.nameBox);
        this.setInitialFocus(this.nameBox);

        // Cancel button on the left below the black box (white text)
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> {
            Minecraft.getInstance().setScreenAndShow(this.parent);
        }).bounds(centerX - boxWidth / 2, centerY + 14, 106, 20).build());

        // Export button on the right below the black box (white initially, green "Exported" after click)
        this.exportButton = Button.builder(Component.literal("Export"), btn -> {
            String typedName = this.nameBox.getValue();
            AunConfigScreen.exportProfileWithCustomName(this.parent, typedName);
            btn.setMessage(Component.literal("§aExported"));
        }).bounds(centerX + boxWidth / 2 - 106, centerY + 14, 106, 20).build();
        this.addRenderableWidget(this.exportButton);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        String titleText = "Name Your Profile";
        int titleX = this.width / 2 - this.font.width(titleText) / 2;
        int titleY = this.height / 2 - 32;
        try {
            var m = graphics.getClass().getMethod("text", net.minecraft.client.gui.Font.class, String.class, int.class, int.class, int.class, boolean.class);
            m.invoke(graphics, this.font, titleText, titleX, titleY, 0xFFFFFFFF, true);
        } catch (Throwable ignored) {}
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(this.parent);
    }
}
// END OF FILE
