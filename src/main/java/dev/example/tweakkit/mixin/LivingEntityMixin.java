package dev.example.tweakkit.mixin;

import dev.example.tweakkit.gamerule.TweakGameRules;
import dev.example.tweakkit.util.MaceTracker;
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
 * Mace tweaks that only ever affect player-vs-player hits (mobs are never touched).
 * "Mace smash" = the damage type the game uses for a mace hit while falling.
 * All behaviour is controlled by the game rules in TweakGameRules, read live on every hit.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    // 1) Damage nerf: after armor and enchantment reductions the final damage is 1 HP.
    @Inject(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"), cancellable = true, require = 0)
    private void tweakkit$nerfMaceOnPlayers(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player
            && source.is(DamageTypes.MACE_SMASH)
            && source.getEntity() instanceof Player
            && self.level() instanceof ServerLevel serverLevel
            && serverLevel.getGameRules().get(TweakGameRules.MACE_NERF_DAMAGE_PLAYER)) {
            cir.setReturnValue(1.0F);
        }
    }

    // 2a) Player A is smashed by player B's mace: remember the pair for this tick.
    @Inject(method = "hurtServer", at = @At("HEAD"), require = 0)
    private void tweakkit$markMaceHit(ServerLevel level, DamageSource source, float amount,
                                      CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player victim
            && source.is(DamageTypes.MACE_SMASH)
            && source.getEntity() instanceof Player attacker
            && level.getGameRules().get(TweakGameRules.MACE_PLAYER_THORNS)) {
            MaceTracker.mark(victim, attacker, level.getGameTime());
        }
    }

    // 2b) Thorns damage from A comes back onto B: re-apply it with extra damage.
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true, require = 0)
    private void tweakkit$boostThorns(ServerLevel level, DamageSource source, float amount,
                                      CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (MaceTracker.isBoosting()
            || !source.is(DamageTypes.THORNS)
            || !MaceTracker.matches(source.getEntity(), self, level.getGameTime())) {
            return;
        }
        int percent = Math.max(0, level.getGameRules().get(TweakGameRules.MACE_PLAYER_THORNS_LEVEL));
        float boosted = amount * (1.0F + percent / 100.0F);

        MaceTracker.setBoosting(true); // the re-applied hit must not be boosted again
        try {
            cir.setReturnValue(self.hurtServer(level, source, boosted));
        } finally {
            MaceTracker.setBoosting(false);
        }
    }
}
