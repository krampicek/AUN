package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Shadow @Final public NonNullList<Slot> slots;

    @Shadow public abstract void doClick(int slotId, int button, ContainerInput input, Player player);

    @Unique private ItemStack aun$ctrlThrowTemplate = ItemStack.EMPTY;
    @Unique private boolean aun$isDroppingAll = false;

    @Inject(method = "doClick", at = @At("HEAD"), require = 0)
    private void aun$captureCtrlThrowTarget(
            int slotId,
            int button,
            ContainerInput input,
            Player player,
            CallbackInfo ci) {
        if (this.aun$isDroppingAll) {
            return;
        }
        this.aun$ctrlThrowTemplate = ItemStack.EMPTY;
        if (AunConfigManager.get().ctrlDropAllSameItems
                && input == ContainerInput.THROW
                && button == 1
                && slotId >= 0
                && slotId < this.slots.size()) {
            Slot clicked = this.slots.get(slotId);
            if (clicked.hasItem() && clicked.mayPickup(player)) {
                this.aun$ctrlThrowTemplate = clicked.getItem().copy();
            }
        }
    }

    @Inject(method = "doClick", at = @At("TAIL"), require = 0)
    private void aun$dropAllMatchingStacks(
            int slotId,
            int button,
            ContainerInput input,
            Player player,
            CallbackInfo ci) {
        if (this.aun$isDroppingAll
                || this.aun$ctrlThrowTemplate.isEmpty()
                || !AunConfigManager.get().ctrlDropAllSameItems
                || input != ContainerInput.THROW
                || button != 1
                || slotId < 0
                || slotId >= this.slots.size()) {
            return;
        }
        Slot originSlot = this.slots.get(slotId);
        ItemStack template = this.aun$ctrlThrowTemplate;
        this.aun$ctrlThrowTemplate = ItemStack.EMPTY;

        this.aun$isDroppingAll = true;
        try {
            for (int i = 0; i < this.slots.size(); i++) {
                if (i == slotId) {
                    continue;
                }
                Slot slot = this.slots.get(i);
                if (slot.container == originSlot.container
                        && slot.hasItem()
                        && slot.mayPickup(player)
                        && ItemStack.isSameItemSameComponents(slot.getItem(), template)) {
                    this.doClick(i, 1, ContainerInput.THROW, player);
                }
            }
        } finally {
            this.aun$isDroppingAll = false;
        }
    }
}
