package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
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
    private void aun$cancelOwnParticle6D(
            ParticleOptions particle, double x, double y, double z,
            double xd, double yd, double zd, CallbackInfo ci) {
        if (aun$isLocalPlayerEffectParticle(particle, x, y, z)) {
            ci.cancel();
        }
    }

    @Inject(
        method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;ZZDDDDDD)V",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void aun$cancelOwnParticle8D(
            ParticleOptions particle, boolean overrideLimiter, boolean alwaysShow,
            double x, double y, double z, double xd, double yd, double zd, CallbackInfo ci) {
        if (aun$isLocalPlayerEffectParticle(particle, x, y, z)) {
            ci.cancel();
        }
    }

    @Unique
    private static boolean aun$isLocalPlayerEffectParticle(ParticleOptions particle, double x, double y, double z) {
        if (!AunClientFeatures.shouldCancelOwnEffectParticle()) return false;
        if (AunClientFeatures.inLocalPlayerTickEffects) return true;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || particle == null) return false;
        String typePath = BuiltInRegistries.PARTICLE_TYPE.getKey(particle.getType()).getPath();
        if (!typePath.contains("effect")) return false;
        double dx = Math.abs(x - mc.player.getX());
        double dy = Math.abs(y - (mc.player.getY() + mc.player.getBbHeight() * 0.5D));
        double dz = Math.abs(z - mc.player.getZ());
        return dx < 1.2D && dy < 1.6D && dz < 1.2D;
    }
}
// END OF FILE
