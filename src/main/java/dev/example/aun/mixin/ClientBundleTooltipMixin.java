package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientBundleTooltip;
import net.minecraft.world.item.component.BundleContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ClientBundleTooltip.class)
public abstract class ClientBundleTooltipMixin {

    @Shadow @Final private BundleContents contents;

    @Inject(method = "slotCount", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$expandSlotCount(CallbackInfoReturnable<Integer> cir) {
        if (AunConfigManager.get().bundleFix) {
            cir.setReturnValue(this.contents.size());
        }
    }

    @Inject(method = "gridSizeY", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$expandGridSizeY(CallbackInfoReturnable<Integer> cir) {
        if (AunConfigManager.get().bundleFix) {
            cir.setReturnValue(Math.max(2, (int) Math.ceil(this.contents.size() / 4.0)));
        }
    }

    @Inject(method = "shouldRenderSurplusText", at = @At("HEAD"), cancellable = true, require = 0)
    private static void aun$hideSurplusText(boolean hasEnoughItems, int column, int row, CallbackInfoReturnable<Boolean> cir) {
        if (AunConfigManager.get().bundleFix) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getAmountOfHiddenItems", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$zeroHiddenItems(List<?> shownItems, CallbackInfoReturnable<Integer> cir) {
        if (AunConfigManager.get().bundleFix) {
            cir.setReturnValue(0);
        }
    }
}
