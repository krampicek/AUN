package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import dev.example.aun.client.AunWorldFavorites;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
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
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
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

    @Unique private Button aun$chestSearchBtn = null;

    @Inject(method = "init()V", at = @At("TAIL"), require = 0)
    private void aun$onScreenInit(CallbackInfo ci) {
        Object self = this;
        this.aun$chestSearchBtn = null;
        AunWorldFavorites.resetChestSearch();

        // BB: Eye toggle button in player InventoryScreen
        if (self instanceof InventoryScreen) {
            if (AunConfigManager.get().hideArmor) {
                int leftPos = (this.width - 176) / 2;
                int topPos = (this.height - 166) / 2;
                Button eyeButton = Button.builder(aun$getEyeLabel(), btn -> {
                    AunClientFeatures.toggleArmorVisualHidden();
                    btn.setMessage(aun$getEyeLabel());
                })
                .bounds(leftPos + 27, topPos + 64, 14, 14)
                .tooltip(Tooltip.create(Component.literal("Toggle Visual Armor (shows for 5s on damage)")))
                .build();
                this.addRenderableWidget(eyeButton);
            }
            return;
        }

        // AQ: Search bar in Chests, Shulker Boxes, Barrels, and Ender Chests
        if (self instanceof AbstractContainerScreen<?> containerScreen
                && !(self instanceof CreativeModeInventoryScreen)
                && AunConfigManager.get().chestSearchBar
                && AunWorldFavorites.isSearchableContainer(containerScreen)) {
            int leftPos = (this.width - 176) / 2;
            int topPos = Math.max(4, (this.height - 166) / 2 - 16);
            AunWorldFavorites.setChestSearchActive(true);
            this.aun$chestSearchBtn = Button.builder(AunWorldFavorites.getSearchButtonLabel(), btn -> {
                AunWorldFavorites.setSearchFocused(!AunWorldFavorites.isSearchFocused());
                btn.setMessage(AunWorldFavorites.getSearchButtonLabel());
            })
            .bounds(leftPos + 82, topPos, 94, 14)
            .tooltip(Tooltip.create(Component.literal("Click to type item search. Right-click or Esc to clear.")))
            .build();
            this.addRenderableWidget(this.aun$chestSearchBtn);
            return;
        }

        // AR: Favourite World button in SelectWorldScreen
        if (self.getClass().getSimpleName().equals("SelectWorldScreen")
                && AunConfigManager.get().favouriteWorlds) {
            int btnX = this.width / 2 + 158;
            int btnY = this.height - 28;
            Button favButton = Button.builder(AunWorldFavorites.getFavoriteButtonLabel((Screen) self), btn -> {
                AunWorldFavorites.cycleSelectedWorldFavorite((Screen) self);
                btn.setMessage(AunWorldFavorites.getFavoriteButtonLabel((Screen) self));
            })
            .bounds(btnX, btnY, 76, 20)
            .tooltip(Tooltip.create(Component.literal(
                    "Click once: Gold (Top) | Twice: Diamond (Best) | Third: Classic")))
            .build();
            this.addRenderableWidget(favButton);
        }
    }

    @Inject(method = "removed()V", at = @At("HEAD"), require = 0)
    private void aun$onScreenRemoved(CallbackInfo ci) {
        AunWorldFavorites.resetChestSearch();
    }

    @Inject(
        method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void aun$onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        Object self = this;

        if (this.aun$chestSearchBtn != null
                && event.button() == 1
                && this.aun$chestSearchBtn.isMouseOver(event.x(), event.y())) {
            AunWorldFavorites.clearSearchQuery();
            this.aun$chestSearchBtn.setMessage(AunWorldFavorites.getSearchButtonLabel());
            cir.setReturnValue(true);
            return;
        }

        if (self instanceof AbstractContainerScreen<?> containerScreen
                && AunConfigManager.get().recipeBookOrigin
                && event.button() == 1) {
            Slot hovered = AunWorldFavorites.findHoveredSlot(containerScreen, event.x(), event.y());
            if (hovered != null && hovered.hasItem() && AunWorldFavorites.isCraftingIngredientSlot(containerScreen, hovered)) {
                ItemStack stack = hovered.getItem();
                if (AunWorldFavorites.showOriginRecipeFor(stack)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }

        if (self instanceof ChatScreen
                && AunConfigManager.get().chatTextCopy
                && (event.button() == 1 || event.hasControlDown())) {
            if (AunWorldFavorites.copyClickedChatLine(event.x(), event.y(), this.height)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(
        method = "charTyped(Lnet/minecraft/client/input/CharacterEvent;)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void aun$onCharTyped(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (this.aun$chestSearchBtn != null && AunWorldFavorites.isSearchFocused()) {
            String ch = event.codepointAsString();
            if (ch != null && !ch.isEmpty() && ch.charAt(0) >= 32) {
                AunWorldFavorites.appendSearchChar(ch);
                this.aun$chestSearchBtn.setMessage(AunWorldFavorites.getSearchButtonLabel());
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(
        method = "keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void aun$onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (this.aun$chestSearchBtn != null && AunWorldFavorites.isSearchFocused()) {
            int key = event.key();
            if (key == 256) {
                AunWorldFavorites.clearSearchQuery();
                AunWorldFavorites.setSearchFocused(false);
                this.aun$chestSearchBtn.setMessage(AunWorldFavorites.getSearchButtonLabel());
                cir.setReturnValue(true);
                return;
            }
            if (key == 257 || key == 335) {
                AunWorldFavorites.setSearchFocused(false);
                this.aun$chestSearchBtn.setMessage(AunWorldFavorites.getSearchButtonLabel());
                cir.setReturnValue(true);
                return;
            }
            if (key == 259) {
                AunWorldFavorites.backspaceSearchChar();
                this.aun$chestSearchBtn.setMessage(AunWorldFavorites.getSearchButtonLabel());
                cir.setReturnValue(true);
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.options != null && mc.options.keyInventory.matches(event)) {
                cir.setReturnValue(true);
            }
        }
    }

    @Unique
    private static Component aun$getEyeLabel() {
        return Component.literal(AunClientFeatures.isArmorVisualHidden() ? "§8👁" : "§a👁");
    }
}
