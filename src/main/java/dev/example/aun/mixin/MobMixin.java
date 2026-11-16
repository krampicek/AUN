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

        if (cfg.essential.noBatSpawning && path.equals("bat") && !self.hasCustomName()) {
            self.discard();
            return;
        }

        if (cfg.mobGriefing.antiEndermanPickup && path.equals("enderman")) {
            try {
                var setCarried = self.getClass().getMethod("setCarriedBlock", net.minecraft.world.level.block.state.BlockState.class);
                setCarried.invoke(self, new Object[]{null});
            } catch (ReflectiveOperationException ignored) {}
        }

        if (cfg.happyGhast.speedHappyGhast && path.equals("happy_ghast")) {
            MobEffectInstance speedEffect = self.getEffect(MobEffects.SPEED);
            if (speedEffect != null) {
                int amp = speedEffect.getAmplifier() + 1;
                Vec3 v = self.getDeltaMovement();
                double nx = v.x, ny = v.y, nz = v.z;

                if (cfg.happyGhast.horizontalSpeedEnabled) {
                    double hFactor = aun$parsePercent(cfg.happyGhast.happyGhastHorizontalBoost) / 100.0D;
                    double maxH = 0.9D * amp * Math.max(0.5D, hFactor);
                    if (Math.hypot(nx, nz) < maxH) {
                        double hMult = 1.0D + (0.08D * amp * hFactor);
                        nx *= hMult;
                        nz *= hMult;
                    }
                }

                if (cfg.happyGhast.verticalSpeedEnabled && Math.abs(ny) > 0.015D) {
                    double vFactor = aun$parsePercent(cfg.happyGhast.happyGhastVerticalBoost) / 100.0D;
                    double maxV = 0.6D * amp * Math.max(0.5D, vFactor);
                    if (Math.abs(ny) < maxV) {
                        double vMult = 1.0D + (0.06D * amp * vFactor);
                        ny *= vMult;
                    }
                }

                self.setDeltaMovement(new Vec3(nx, ny, nz));
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
