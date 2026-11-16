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
import net.minecraft.network.chat.Component;
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
            // Standalone Save Button (saves without quitting Cloth Config)
            this.addRenderableWidget(Button.builder(Component.literal("§6Save"), btn -> {
                AunConfigScreen.saveWithoutClosing(self);
                btn.setMessage(Component.literal("§aSaved!"));
            })
            .bounds(this.width / 2 + 158, this.height - 26, 48, 20)
            .tooltip(Tooltip.create(Component.literal("Save changes without closing the menu")))
            .build());

            // Link Own Profile Button (opens desktop file browser)
            this.addRenderableWidget(Button.builder(Component.literal("§b📂 Link Profile"), btn -> {
                AunConfigScreen.openProfileFileChooser(self, null);
            })
            .bounds(this.width / 2 - 236, this.height - 26, 78, 20)
            .tooltip(Tooltip.create(Component.literal("Open desktop file browser to link your own profile (.json)")))
            .build());

            // Export Own Profile Button (exports to .minecraft / Prism folder and opens Windows Explorer)
            this.addRenderableWidget(Button.builder(Component.literal("§d📤 Export"), btn -> {
                AunConfigScreen.exportOwnProfile(self);
                btn.setMessage(Component.literal("§aExported!"));
            })
            .bounds(this.width / 2 + 210, this.height - 26, 62, 20)
            .tooltip(Tooltip.create(Component.literal("Export current settings to aun_exported_profile.json and open folder")))
            .build());
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
    private static Component
