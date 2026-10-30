package dev.example.tweakkit.mixin;

import dev.example.tweakkit.config.TweakConfigManager;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Feature 3: LightningBolt#spawnFire is the only place a strike places fire blocks.
 * Cancelling it keeps the bolt, sound, flash, damage and lightning-rod behaviour.
 */
@Mixin(LightningBolt.class)
public abstract class LightningBoltMixin {

    @Inject(method = "spawnFire", at = @At("HEAD"), cancellable = true)
    private void tweakkit$noFire(int spreadAttempts, CallbackInfo ci) {
        if (TweakConfigManager.get().safeLightning) {
            ci.cancel();
        }
    }
}
