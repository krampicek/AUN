package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow public abstract double distanceToSqr(Entity entity);
    @Shadow public abstract Vec3 getDeltaMovement();
    @Shadow public abstract void setDeltaMovement(Vec3 deltaMovement);
    @Shadow public abstract AABB getBoundingBox();
    @Shadow public abstract Level level();

    @Inject(method = "push(DDD)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$cancelStackedMinecartPush(double x, double y, double z, CallbackInfo ci) {
        if (!AunConfigManager.get().stackedMinecartFix) {
            return;
        }
        Entity self = (Entity) (Object) this;
        if (aun$isMinecart(self)) {
            List<Entity> overlapping = this.level().getEntities(
                    self,
                    this.getBoundingBox().deflate(0.15D),
                    e -> aun$isMinecart(e) && this.distanceToSqr(e) < 0.36D);
            if (!overlapping.isEmpty()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "baseTick", at = @At("TAIL"), require = 0)
    private void aun$dampStackedMinecartMomentum(CallbackInfo ci) {
        if (!AunConfigManager.get().stackedMinecartFix) {
            return;
        }
        Entity self = (Entity) (Object) this;
        if (aun$isMinecart(self)) {
            List<Entity> overlapping = this.level().getEntities(
                    self,
                    this.getBoundingBox().deflate(0.15D),
                    e -> aun$isMinecart(e) && this.distanceToSqr(e) < 0.36D);
            if (!overlapping.isEmpty()) {
                Vec3 v = this.getDeltaMovement();
                this.setDeltaMovement(new Vec3(v.x * 0.65D, v.y, v.z * 0.65D));
            }
        }
    }

    @Unique
    private static boolean aun$isMinecart(Entity entity) {
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        return path.contains("minecart");
    }
}
