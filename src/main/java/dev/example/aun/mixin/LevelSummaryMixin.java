package dev.example.aun.mixin;

import dev.example.aun.client.AunWorldFavorites;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelSummary.class)
public abstract class LevelSummaryMixin {

    @Inject(
        method = "compareTo(Lnet/minecraft/world/level/storage/LevelSummary;)I",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void aun$sortFavoriteWorldsOnTop(LevelSummary other, CallbackInfoReturnable<Integer> cir) {
        if (!AunConfigManager.get().favouriteWorlds || other == null) {
            return;
        }
        String myId = AunWorldFavorites.getLevelIdFromSummary(this);
        String otherId = AunWorldFavorites.getLevelIdFromSummary(other);
        int myTier = AunWorldFavorites.getWorldTier(myId);
        int otherTier = AunWorldFavorites.getWorldTier(otherId);
        if (myTier != otherTier) {
            cir.setReturnValue(Integer.compare(otherTier, myTier));
        }
    }

    @Inject(method = "getLevelName", at = @At("RETURN"), cancellable = true, require = 0)
    private void aun$decorateFavoriteWorldName(CallbackInfoReturnable<String> cir) {
        if (!AunConfigManager.get().favouriteWorlds) {
            return;
        }
        String myId = AunWorldFavorites.getLevelIdFromSummary(this);
        int tier = AunWorldFavorites.getWorldTier(myId);
        if (tier == 1) {
            cir.setReturnValue("§6★ [GOLD] §r" + cir.getReturnValue());
        } else if (tier == 2) {
            cir.setReturnValue("§b❖ [DIAMOND] §r" + cir.getReturnValue());
        }
    }
}
