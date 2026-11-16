package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import dev.example.aun.client.AunConfigScreen;
import dev.example.aun.client.AunWorldFavorites;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Shadow public int width;
    @Shadow public int height;

    @Shadow
    protected abstract <T extends GuiEventListener & Renderable & NarratableEntry> T addRenderableWidget(T widget);

    @Inject(
        method = {"init(II)V", "init(Lnet/minecraft/client/Minecraft;II)V", "rebuildWidgets()V"},
        at = @At("TAIL"),
        require = 0
    )
    private void aun$onScreenInitLifecycle(CallbackInfo ci) {
        Screen self = (Screen) (Object) this;
        AunClientFeatures.currentScreen = self;
        AunWorldFavorites.resetChestSearch();

        if (AunConfigScreen.isClothConfigScreen(self)) {
            AunConfigManager.ensureProfileFoldersExist();

            // 1. Bottom-left button: "Browse"
            Button browseBtn = Button.builder(Component.literal("Browse"), btn -> {
                AunConfigScreen.openFolderInOS(AunConfigScreen.getProfilesBaseDir());
            })
            .bounds(this.width / 2 - 204, this.height - 26, 80, 20)
            .tooltip(Tooltip.create(Component.literal("Open the Aun_profiles folder in your file explorer")))
            .build();
            this.addRenderableWidget(browseBtn);

            // 2. Center-right button: "Save & Quit" (always bright & clickable like Save)
            Button saveQuitBtn = Button.builder(Component.literal("Save & Quit"), btn -> {
                AunConfigScreen.saveAndClose(self);
            })
            .bounds(this.width / 2 - 12, this.height - 26, 106, 20)
            .tooltip(Tooltip.create(Component.literal("Save changes and close the menu")))
            .build();
            this.addRenderableWidget(saveQuitBtn);

            // 3. Bottom-right button: "Save"
            Button saveBtn = Button.builder(Component.literal("Save"), btn -> {
                AunConfigScreen.saveWithoutClosing(self);
            })
            .bounds(this.width / 2 + 100, this.height - 26, 80, 20)
            .tooltip(Tooltip.create(Component.literal("Save changes without closing the menu")))
            .build();
            this.addRenderableWidget(saveBtn);

            // 4. Selectable ▼ arrow button on the left side of the Loadable Profile black box
            Button arrowBtn = Button.builder(Component.literal("▼"), btn -> {
                AunConfigScreen.dropdownExpanded = !AunConfigScreen.dropdownExpanded;
            }).bounds(0, 0, AunConfigScreen.ARROW_BTN_WIDTH, 20).build();
            arrowBtn.visible = false;
            arrowBtn.active = false;
            this.addRenderableWidget(arrowBtn);

            // 5. 5 scrollable dropdown item buttons (max 5 visible at once)
            List<Button> itemBtns = new ArrayList<>();
            for (int i = 0; i < AunConfigScreen.MAX_VISIBLE_PROFILES; i++) {
                final int slotIdx = i;
                Button itemBtn = Button.builder(Component.literal(""), btn -> {
                    AunConfigScreen.selectProfileFromDropdown(self, slotIdx);
                }).bounds(0, 0, AunConfigScreen.PROFILE_BOX_WIDTH + AunConfigScreen.ARROW_BTN_WIDTH, 18).build();
                itemBtn.visible = false;
                itemBtn.active = false;
                this.addRenderableWidget(itemBtn);
                itemBtns.add(itemBtn);
            }

            AunConfigScreen.registerClothOverlayWidgets(browseBtn, saveQuitBtn, saveBtn, arrowBtn, itemBtns);
            AunConfigScreen.layoutBottomBarButtons(self);
            return;
        }

        if (self instanceof InventoryScreen invScreen) {
            if (AunConfigManager.get().playerHud.hideArmor) {
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

        if (self instanceof AbstractContainerScreen<?> containerScreen
                && !(self instanceof CreativeModeInventoryScreen)
                && AunConfigManager.get().remainingHuds.chestSearchBar
                && AunWorldFavorites.isSearchableContainer(containerScreen)) {
            Minecraft mc = Minecraft.getInstance();
            int leftPos = AunWorldFavorites.getContainerLeftPos(containerScreen);
            int topPos = AunWorldFavorites.getContainerTopPos(containerScreen);
            AunWorldFavorites.setChestSearchActive(true);
            EditBox box = new EditBox(mc.font, leftPos + 80, topPos + 4, 88, 11, Component.literal("Search"));
            box.setMaxLength(24);
            box.setHint(Component.literal("§7Search..."));
            box.setResponder(AunWorldFavorites::setSearchQuery);
            AunWorldFavorites.activeChestSearchBox = box;
            this.addRenderableWidget(box);
            return;
        }

        if (self.getClass().getSimpleName().equals("SelectWorldScreen")
                && AunConfigManager.get().worldSettings.favouriteWorlds) {
            int btnX = this.width / 2 + 104;
            int btnY = 22;
            Button favButton = Button.builder(AunWorldFavorites.getFavoriteButtonLabel(self), btn -> {
                AunWorldFavorites.cycleSelectedWorldFavorite(self);
                btn.setMessage(AunWorldFavorites.getFavoriteButtonLabel(self));
            })
            .bounds(btnX, btnY, 76, 20)
            .tooltip(Tooltip.create(Component.literal(
                    "Select a world and click: 1x = Gold (Top) | 2x = Diamond (Best) | 3x = Classic")))
            .build();
            this.addRenderableWidget(favButton);
        }
    }

    @Inject(
        method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void aun$onClothScreenMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (AunConfigScreen.handleCustomMouseClick((Screen) (Object) this, event.x(), event.y(), event.button())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(
        method = "mouseScrolled(DDDD)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void aun$onClothScreenMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        if (AunConfigScreen.onClothScreenMouseScrolled((Screen) (Object) this, mouseX, mouseY, scrollY)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "removed()V", at = @At("HEAD"), require = 0)
    private void aun$onScreenRemoved(CallbackInfo ci) {
        if (AunClientFeatures.currentScreen == (Object) this) {
            AunClientFeatures.currentScreen = null;
        }
        AunWorldFavorites.resetChestSearch();
    }

    @Inject(
        method = "charTyped(Lnet/minecraft/client/input/CharacterEvent;)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void aun$onCharTyped(CharacterEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (AunWorldFavorites.onScreenCharTyped((Screen) (Object) this, event)) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private static Component aun$getEyeLabel() {
        return Component.literal(AunClientFeatures.isArmorVisualHidden() ? "§8👁" : "§a👁");
    }
}
// END OF FILE
