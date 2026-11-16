package dev.example.aun.mixin;

import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMixin extends LivingEntity {

    @Unique private double aun$prevHorizontalSpeed = 0.0D;

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
            Vec3 v = self.getDeltaMovement();
            double curH = Math.hypot(v.x, v.z);
            boolean riderSteering = self.getControllingPassenger() instanceof Player rider
                    && (Math.abs(rider.xxa) > 0.01F || Math.abs(rider.zza) > 0.01F || curH > this.aun$prevHorizontalSpeed * 0.96D);

            if (speedEffect != null && riderSteering && curH > 0.02D) {
                int amp = speedEffect.getAmplifier() + 1;
                double nx = v.x, ny = v.y, nz = v.z;

                if (cfg.happyGhast.horizontalSpeedEnabled) {
                    double hFactor = aun$parsePercent(cfg.happyGhast.happyGhastHorizontalBoost) / 100.0D;
                    double maxH = 0.65D * amp * Math.max(0.25D, hFactor);
                    if (curH < maxH) {
                        double hMult = 1.0D + (0.06D * amp * hFactor);
                        nx *= hMult;
                        nz *= hMult;
                    }
                }

                if (cfg.happyGhast.verticalSpeedEnabled && Math.abs(ny) > 0.025D) {
                    double vFactor = aun$parsePercent(cfg.happyGhast.happyGhastVerticalBoost) / 100.0D;
                    double maxV = 0.45D * amp * Math.max(0.25D, vFactor);
                    if (Math.abs(ny) < maxV) {
                        double vMult = 1.0D + (0.05D * amp * vFactor);
                        ny *= vMult;
                    }
                }

                self.setDeltaMovement(new Vec3(nx, ny, nz));
                this.aun$prevHorizontalSpeed = Math.hypot(nx, nz);
            } else {
                this.aun$prevHorizontalSpeed = curH;
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
