package dev.example.aun.mixin;

import dev.example.aun.client.AunWorldFavorites;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class SlotMixin {

    @Shadow @Final public Container container;
    @Shadow public abstract ItemStack getItem();

    @Inject(method = "isActive", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$filterChestSlotsBySearch(CallbackInfoReturnable<Boolean> cir) {
        if (!AunWorldFavorites.isChestSearchActive()) {
            return;
        }
        if (this.container instanceof Inventory) {
            return;
        }
        if (!AunWorldFavorites.matchesSearch(this.getItem())) {
            cir.setReturnValue(false);
        }
    }
}
