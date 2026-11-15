package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantOffer.class)
public abstract class MerchantOfferMixin {

    @Shadow public abstract void resetUses();

    @Inject(method = "isOutOfStock", at = @At("HEAD"), cancellable = true)
    private void aun$neverOutOfStock(CallbackInfoReturnable<Boolean> cir) {
        if (AunConfigManager.get().infiniteVillagerTrading) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "increaseUses", at = @At("TAIL"))
    private void aun$resetUsesAfterTrade(CallbackInfo ci) {
        if (AunConfigManager.get().infiniteVillagerTrading) {
            this.resetUses();
        }
    }
}
