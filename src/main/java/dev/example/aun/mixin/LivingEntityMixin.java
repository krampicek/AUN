package dev.example.aun.mixin;

import dev.example.aun.gamerule.AunGameRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mace nerf (player vs player only): when the game rule aun:macenerfdmgplayer is true,
 * a mace smash from a player deals exactly 1 HP to another player, after armor.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"), cancellable = true, require = 0)
    private void aun$nerfMaceOnPlayers(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player
            && source.is(DamageTypes.MACE_SMASH)
            && source.getEntity() instanceof Player
            && self.level() instanceof ServerLevel serverLevel
            && serverLevel.getGameRules().get(AunGameRules.MACE_NERF_DAMAGE_PLAYER)) {
            cir.setReturnValue(1.0F);
        }
    }
}
