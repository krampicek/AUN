package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

    @Inject(
        method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void aun$hideOwnEffectParticles(
            ParticleOptions options,
            double x,
            double y,
            double z,
            double xd,
            double yd,
            double zd,
            CallbackInfo ci) {
        if (AunClientFeatures.shouldCancelOwnEffectParticle()) {
            ci.cancel();
        }
    }
}
