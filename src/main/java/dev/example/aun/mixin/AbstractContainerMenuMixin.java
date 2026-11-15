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

    @Unique private ItemStack aun$ctrlThrowTemplate = ItemStack.EMPTY;

    @Inject(method = "doClick", at = @At("HEAD"), require = 0)
    private void aun$captureCtrlThrowTarget(
            int slotId,
            int button,
            ContainerInput input,
            Player player,
            CallbackInfo ci) {
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
        if (this.aun$ctrlThrowTemplate.isEmpty()
                || !AunConfigManager.get().ctrlDropAllSameItems
                || input != ContainerInput.THROW
                || button != 1
                || slotId < 0
                || slotId >= this.slots.size()) {
            this.aun$ctrlThrowTemplate = ItemStack.EMPTY;
            return;
        }
        Slot originSlot = this.slots.get(slotId);
        ItemStack template = this.aun$ctrlThrowTemplate;
        this.aun$ctrlThrowTemplate = ItemStack.EMPTY;

        for (Slot slot : this.slots) {
            if (slot.container == originSlot.container
                    && slot.hasItem()
                    && slot.mayPickup(player)
                    && ItemStack.isSameItemSameComponents(slot.getItem(), template)) {
                ItemStack removed = slot.safeTake(slot.getItem().getCount(), Integer.MAX_VALUE, player);
                if (!removed.isEmpty()) {
                    player.drop(removed, false, true);
                }
            }
        }
    }
}
