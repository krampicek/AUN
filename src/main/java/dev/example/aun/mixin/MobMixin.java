package dev.example.aun.mixin;

import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMixin extends LivingEntity {

    protected MobMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void aun$onMobTick(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        AunConfig cfg = AunConfigManager.get();
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(self.getType()).getPath();

        if (cfg.noBatSpawning && path.equals("bat") && !self.hasCustomName()) {
            self.discard();
            return;
        }

        if (cfg.mobGriefing.antiEndermanPickup && path.equals("enderman")) {
            try {
                var setCarried = self.getClass().getMethod("setCarriedBlock", net.minecraft.world.level.block.state.BlockState.class);
                setCarried.invoke(self, new Object[]{null});
            } catch (ReflectiveOperationException ignored) {}
        }

        if (cfg.speedHappyGhast && path.equals("happy_ghast")) {
            MobEffectInstance speedEffect = self.getEffect(MobEffects.SPEED);
            if (speedEffect != null || self.isVehicle()) {
                int amp = speedEffect != null ? speedEffect.getAmplifier() + 1 : 1;
                double hBoost = aun$parsePercent(cfg.happyGhastHorizontalBoost) / 100.0D;
                double vBoost = aun$parsePercent(cfg.happyGhastVerticalBoost) / 100.0D;
                Vec3 v = self.getDeltaMovement();
                double hMult = 1.0D + (0.08D * amp * hBoost);
                double vMult = 1.0D + (0.08D * amp * vBoost);
                self.setDeltaMovement(new Vec3(v.x * hMult, v.y * vMult, v.z * hMult));
            }
        }
    }

    @Unique
    private static double aun$parsePercent(String val) {
        if (val == null || val.isBlank()) return 100.0D;
        try { return Math.max(0.0D, Math.min(500.0D, Double.parseDouble(val.trim()))); }
        catch (NumberFormatException e) { return 100.0D; }
    }
}
// END OF FILE
