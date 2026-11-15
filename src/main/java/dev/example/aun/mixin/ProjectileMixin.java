package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Projectile.class)
public abstract class ProjectileMixin {

    @Inject(method = "onHit", at = @At("HEAD"), require = 0)
    private void aun$onProjectileHitStart(HitResult result, CallbackInfo ci) {
        Projectile self = (Projectile) (Object) this;
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(self.getType()).getPath();
        if (path.equals("fireball")) {
            AunClientFeatures.inGhastFireballHit = true;
        }
    }

    @Inject(method = "onHit", at = @At("RETURN"), require = 0)
    private void aun$onProjectileHitEnd(HitResult result, CallbackInfo ci) {
        AunClientFeatures.inGhastFireballHit = false;
    }

    @Inject(method = "onHitBlock", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$preventBlazeFireOnBlock(BlockHitResult result, CallbackInfo ci) {
        if (!AunConfigManager.get().mobGriefing.antiBlazeGrief) {
            return;
        }
        Projectile self = (Projectile) (Object) this;
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(self.getType()).getPath();
        if (path.equals("small_fireball")) {
            ci.cancel();
        }
    }
}
// END OF FILE
