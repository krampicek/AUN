package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import dev.example.aun.client.AunWorldFavorites;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Shadow public int width;
    @Shadow public int height;

    @Shadow
    protected abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget);

    @Unique private EditBox aun$chestSearchBox = null;

    @Inject(
        method = {"init(II)V", "init(Lnet/minecraft/client/Minecraft;II)V", "rebuildWidgets()V"},
        at = @At("TAIL"),
        require = 0
    )
    private void aun$onScreenInitLifecycle(CallbackInfo ci) {
        Object self = this;
        AunClientFeatures.currentScreen = (Screen) self;
        this.aun$chestSearchBox = null;
        AunWorldFavorites.resetChestSearch();

        if (self instanceof InventoryScreen invScreen) {
            if (AunConfigManager.get().hud.hideArmor) {
                int leftPos = AunWorldFavorites.getContainerLeftPos(invScreen);
                int topPos = AunWorldFavorites.getContainerTopPos(invScreen);
                Button eyeButton = Button.builder(aun$getEyeLabel(), btn -> {
                    AunClientFeatures.toggleArmorVisualHidden();
                    btn.setMessage(aun$getEyeLabel());
                })
                .bounds(leftPos + 27, topPos + 62, 16, 14)
                .tooltip(Tooltip.create(Component.literal("Toggle Visual Armor (shows for 5s on damage)")))
                .build();
                this.addRenderableWidget(eyeButton);
            }
            return;
        }

        // AQ: Typeable Search Box inside the Chest header bar next to "Chest" (IMAGE - B)
        if (self instanceof AbstractContainerScreen<?> containerScreen
                && !(self instanceof CreativeModeInventoryScreen)
                && AunConfigManager.get().chestSearchBar
                && AunWorldFavorites.isSearchableContainer(containerScreen)) {
            Minecraft mc = Minecraft.getInstance();
            int leftPos = AunWorldFavorites.getContainerLeftPos(containerScreen);
            int topPos = AunWorldFavorites.getContainerTopPos(containerScreen);
            AunWorldFavorites.setChestSearchActive(true);
            this.aun$chestSearchBox = new EditBox(mc.font, leftPos + 80, topPos + 5, 88, 10, Component.literal("Search"));
            this.aun$chestSearchBox.setMaxLength(24);
            this.aun$chestSearchBox.setHint(Component.literal("§7Search..."));
            this.aun$chestSearchBox.setResponder(AunWorldFavorites::setSearchQuery);
            this.addRenderableWidget(this.aun$chestSearchBox);
            return;
        }

        if (self.getClass().getSimpleName().equals("SelectWorldScreen")
                && AunConfigManager.get().worldSettings.favouriteWorlds) {
            int btnX = this.width / 2 + 104;
            int btnY = 22;
            Button favButton = Button.builder(AunWorldFavorites.getFavoriteButtonLabel((Screen) self), btn -> {
                AunWorldFavorites.cycleSelectedWorldFavorite((Screen) self);
                btn.setMessage(AunWorldFavorites.getFavoriteButtonLabel((Screen) self));
            })
            .bounds(btnX, btnY, 76, 20)
            .tooltip(Tooltip.create(Component.literal(
                    "Select a world and click: 1x = Gold (Top) | 2x = Diamond (Best) | 3x = Classic")))
            .build();
            this.addRenderableWidget(favButton);
        }
    }

    @Inject(method = "removed()V", at = @At("HEAD"), require = 0)
    private void aun$onScreenRemoved(CallbackInfo ci) {
        if (AunClientFeatures.currentScreen == (Object) this) {
            AunClientFeatures.currentScreen = null;
        }
        AunWorldFavorites.resetChestSearch();
    }

    @Unique
    public boolean aun$handleSubScreenMouseClick(MouseButtonEvent event) {
        Object self = this;

        if (this.aun$chestSearchBox != null) {
            if (this.aun$chestSearchBox.isMouseOver(event.x(), event.y())) {
                if (event.button() == 1) {
                    this.aun$chestSearchBox.setValue("");
                }
                this.aun$chestSearchBox.setFocused(true);
                return true;
            } else {
                this.aun$chestSearchBox.setFocused(false);
            }
        }

        // AH: Right-click crafting grid ingredient to link directly in the Recipe Book
        if (self instanceof AbstractContainerScreen<?> containerScreen
                && AunConfigManager.get().hud.recipeBookOrigin
                && event.button() == 1) {
            Slot hovered = AunWorldFavorites.findHoveredSlot(containerScreen, event.x(), event.y());
            if (hovered != null && hovered.hasItem() && !(hovered.container instanceof Inventory)) {
                if (AunWorldFavorites.linkRecipeBookToIngredient(containerScreen, hovered.getItem())) {
                    return true;
                }
            }
        }

        // X: Click sent chat message to load into chat box for highlighting & copying
        if (self instanceof ChatScreen chatScreen
                && AunConfigManager.get().hud.chatTextCopy
                && event.y() < this.height - 28) {
            return AunWorldFavorites.selectClickedChatLineForHighlight(chatScreen, event.x(), event.y(), this.height);
        }
        return false;
    }

    @Unique
    public boolean aun$handleSubScreenKeyPressed(KeyEvent event) {
        if (this.aun$chestSearchBox != null && this.aun$chestSearchBox.isFocused()) {
            if (event.key() == 256) {
                this.aun$chestSearchBox.setFocused(false);
                return true;
            }
            this.aun$chestSearchBox.keyPressed(event);
            return true;
        }
        return false;
    }

    @Inject(
        method = "charTyped(Lnet/minecraft/client/input/CharacterEvent;)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void aun$onCharTyped(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (this.aun$chestSearchBox != null && this.aun$chestSearchBox.isFocused()) {
            if (this.aun$chestSearchBox.charTyped(event)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Unique
    private static Component aun$getEyeLabel() {
        return Component.literal(AunClientFeatures.isArmorVisualHidden() ? "§8👁" : "§a👁");
    }
}
// END OF FILE
