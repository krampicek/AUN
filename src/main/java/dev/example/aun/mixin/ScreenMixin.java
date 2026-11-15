package dev.example.aun.mixin;

import com.mojang.blaze3d.platform.InputConstants;
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
    @Unique private Button aun$recipeOriginBtn = null;

    @Inject(
        method = {"init(II)V", "init(Lnet/minecraft/client/Minecraft;II)V", "rebuildWidgets()V"},
        at = @At("TAIL"),
        require = 0
    )
    private void aun$onScreenInitLifecycle(CallbackInfo ci) {
        Object self = this;
        this.aun$chestSearchBtn = null;
        this.aun$recipeOriginBtn = null;
        AunWorldFavorites.resetChestSearch();

        // BB: Eye toggle button in player InventoryScreen
        if (self instanceof InventoryScreen invScreen) {
            if (AunConfigManager.get().hideArmor) {
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

        // Permanent Horse Info inside HorseInventoryScreen while riding a horse/donkey/mule
        if (AunConfigManager.get().horseInfo && AunWorldFavorites.isHorseInventoryScreen((Screen) self)) {
            AbstractContainerScreen<?> horseScreen = (AbstractContainerScreen<?>) self;
            int leftPos = AunWorldFavorites.getContainerLeftPos(horseScreen);
            int topPos = Math.max(2, AunWorldFavorites.getContainerTopPos(horseScreen) - 16);
            Button infoBanner = Button.builder(AunWorldFavorites.getMountedHorseInfoLabel(), btn -> {})
                    .bounds(leftPos, topPos, 176, 14)
                    .build();
            this.addRenderableWidget(infoBanner);
            return;
        }

        // AQ: Search bar in Chests, Shulker Boxes, Barrels, and Ender Chests
        if (self instanceof AbstractContainerScreen<?> containerScreen
                && !(self instanceof CreativeModeInventoryScreen)
                && AunConfigManager.get().chestSearchBar
                && AunWorldFavorites.isSearchableContainer(containerScreen)) {
            int leftPos = AunWorldFavorites.getContainerLeftPos(containerScreen);
            int topPos = Math.max(2, AunWorldFavorites.getContainerTopPos(containerScreen) - 16);
            AunWorldFavorites.setChestSearchActive(true);
            this.aun$chestSearchBtn = Button.builder(AunWorldFavorites.getSearchButtonLabel(), btn -> {
                AunWorldFavorites.setSearchFocused(!AunWorldFavorites.isSearchFocused());
                btn.setMessage(AunWorldFavorites.getSearchButtonLabel());
            })
            .bounds(leftPos + 80, topPos, 96, 14)
            .tooltip(Tooltip.create(Component.literal("Click to search items. Right-click or Esc to clear.")))
            .build();
            this.addRenderableWidget(this.aun$chestSearchBtn);
            return;
        }

        // AR: Favourite World button in SelectWorldScreen
        if (self.getClass().getSimpleName().equals("SelectWorldScreen")
                && AunConfigManager.get().favouriteWorlds) {
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
        AunWorldFavorites.resetChestSearch();
    }

    @Unique
    public boolean aun$handleSubScreenMouseClick(MouseButtonEvent event) {
        Object self = this;

        if (this.aun$chestSearchBtn != null
                && event.button() == 1
                && this.aun$chestSearchBtn.isMouseOver(event.x(), event.y())) {
            AunWorldFavorites.clearSearchQuery();
            this.aun$chestSearchBtn.setMessage(AunWorldFavorites.getSearchButtonLabel());
            return true;
        }

        if (self instanceof AbstractContainerScreen<?> containerScreen
                && AunConfigManager.get().recipeBookOrigin
                && event.button() == 1) {
            Slot hovered = AunWorldFavorites.findHoveredSlot(containerScreen, event.x(), event.y());
            if (hovered != null && hovered.hasItem()) {
                ItemStack stack = hovered.getItem();
                if (AunWorldFavorites.showOriginRecipeFor(stack)) {
                    int leftPos = AunWorldFavorites.getContainerLeftPos(containerScreen);
                    int topPos = Math.max(2, AunWorldFavorites.getContainerTopPos(containerScreen) - 16);
                    if (this.aun$recipeOriginBtn == null) {
                        this.aun$recipeOriginBtn = Button.builder(
                                Component.literal(AunWorldFavorites.getActiveRecipeOriginBanner()), b -> {})
                                .bounds(Math.max(4, leftPos - 30), topPos, 236, 14)
                                .build();
                        this.addRenderableWidget(this.aun$recipeOriginBtn);
                    } else {
                        this.aun$recipeOriginBtn.setMessage(
                                Component.literal(AunWorldFavorites.getActiveRecipeOriginBanner()));
                    }
                    return true;
                }
            }
        }

        if (self instanceof ChatScreen
                && AunConfigManager.get().chatTextCopy
                && (event.button() == 1 || event.hasControlDown())) {
            return AunWorldFavorites.copyClickedChatLine(event.x(), event.y(), this.height);
        }
        return false;
    }

    @Unique
    public boolean aun$handleSubScreenKeyPressed(KeyEvent event) {
        Object self = this;
        Minecraft mc = Minecraft.getInstance();

        if (this.aun$chestSearchBtn != null && AunWorldFavorites.isSearchFocused()) {
            int key = event.key();
            if (key == 256) {
                AunWorldFavorites.clearSearchQuery();
                AunWorldFavorites.setSearchFocused(false);
                this.aun$chestSearchBtn.setMessage(AunWorldFavorites.getSearchButtonLabel());
                return true;
            }
            if (key == 257 || key == 335) {
                AunWorldFavorites.setSearchFocused(false);
                this.aun$chestSearchBtn.setMessage(AunWorldFavorites.getSearchButtonLabel());
                return true;
            }
            if (key == 259) {
                AunWorldFavorites.backspaceSearchChar();
                this.aun$chestSearchBtn.setMessage(AunWorldFavorites.getSearchButtonLabel());
                return true;
            }
            if (mc != null && mc.options != null && mc.options.keyInventory.matches(event)) {
                return true;
            }
        }

        // BF: Sprint/Ctrl + Drop (Q) drops all stacks of the hovered item
        if (self instanceof AbstractContainerScreen<?> containerScreen
                && AunConfigManager.get().ctrlDropAllSameItems
                && mc != null
                && mc.options != null
                && mc.options.keyDrop.matches(event)) {
            boolean sprintOrCtrlHeld = event.hasControlDown()
                    || InputConstants.isKeyDown(mc.getWindow(), InputConstants.KEY_LCONTROL)
                    || mc.options.keySprint.isDown();
            if (sprintOrCtrlHeld && AunWorldFavorites.handleSprintDropAllInContainer(containerScreen)) {
                return true;
            }
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
        if (this.aun$chestSearchBtn != null && AunWorldFavorites.isSearchFocused()) {
            String ch = event.codepointAsString();
            if (ch != null && !ch.isEmpty() && ch.charAt(0) >= 32) {
                AunWorldFavorites.appendSearchChar(ch);
                this.aun$chestSearchBtn.setMessage(AunWorldFavorites.getSearchButtonLabel());
                cir.setReturnValue(true);
            }
        }
    }

    @Unique
    private static Component aun$getEyeLabel() {
        return Component.literal(AunClientFeatures.isArmorVisualHidden() ? "§8👁" : "§a👁");
    }
}
