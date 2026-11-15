package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityClientMixin {

    @Inject(method = "tickEffects", at = @At("HEAD"), require = 0)
    private void aun$markOwnEffectParticlesStart(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && (Object) this == client.player) {
            AunClientFeatures.inLocalPlayerTickEffects = true;
        }
    }

    @Inject(method = "tickEffects", at = @At("RETURN"), require = 0)
    private void aun$markOwnEffectParticlesEnd(CallbackInfo ci) {
        AunClientFeatures.inLocalPlayerTickEffects = false;
    }
}
