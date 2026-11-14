package dev.example.aun.mixin;

import dev.example.aun.Aun;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Items a player drops (tossed, or all items on death) never run out their despawn timer.
 * Everything else in the world despawns normally.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {

    @Inject(
        method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;",
        at = @At("RETURN"),
        require = 0
    )
    private void aun$persistentDrops(ItemStack stack, boolean randomly, boolean thrower,
                                     CallbackInfoReturnable<ItemEntity> cir) {
        ItemEntity item = cir.getReturnValue();
        if (item != null && AunConfigManager.get().playerDropsNeverDespawn) {
            item.setUnlimitedLifetime();
            item.addTag(Aun.DROP_TAG);
            item.addTag(Aun.sessionTag());
        }
    }
}
