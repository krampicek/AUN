package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Shadow public int width;
    @Shadow public int height;

    @Shadow
    protected abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget);

    @Inject(method = "init()V", at = @At("TAIL"), require = 0)
    private void aun$addInventoryArmorEyeToggle(CallbackInfo ci) {
        if (!AunConfigManager.get().hideArmor || !((Object) this instanceof InventoryScreen)) {
            return;
        }
        int leftPos = (this.width - 176) / 2;
        int topPos = (this.height - 166) / 2;
        int btnX = leftPos + 27;
        int btnY = topPos + 64;

        Button eyeButton = Button.builder(aun$getEyeLabel(), btn -> {
            AunClientFeatures.toggleArmorVisualHidden();
            btn.setMessage(aun$getEyeLabel());
        })
        .bounds(btnX, btnY, 14, 14)
        .tooltip(Tooltip.create(Component.literal("Toggle Visual Armor (shows for 5s on damage)")))
        .build();

        this.addRenderableWidget(eyeButton);
    }

    @Unique
    private static Component aun$getEyeLabel() {
        return Component.literal(AunClientFeatures.isArmorVisualHidden() ? "§8👁" : "§a👁");
    }
}
