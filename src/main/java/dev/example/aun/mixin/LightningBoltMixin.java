package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.world.entity.LightningBolt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Feature 3: LightningBolt#spawnFire is the only place a strike places fire. Cancel it. */
@Mixin(LightningBolt.class)
public abstract class LightningBoltMixin {

    @Inject(method = "spawnFire", at = @At("HEAD"), cancellable = true)
    private void aun$noFire(int spreadAttempts, CallbackInfo ci) {
        if (AunConfigManager.get().safeLightning) {
            ci.cancel();
        }
    }
}
