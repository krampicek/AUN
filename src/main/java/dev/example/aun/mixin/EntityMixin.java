package dev.example.aun.mixin;

import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow public abstract double distanceToSqr(Entity entity);
    @Shadow public abstract Vec3 getDeltaMovement();
    @Shadow public abstract void setDeltaMovement(Vec3 deltaMovement);

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$fixStackedMinecartMomentum(Entity other, CallbackInfo ci) {
        if (!AunConfigManager.get().stackedMinecartFix || other == null) {
            return;
        }
        Entity self = (Entity) (Object) this;
        if (aun$isMinecart(self) && aun$isMinecart(other) && this.distanceToSqr(other) < 0.25D) {
            Vec3 v = this.getDeltaMovement();
            this.setDeltaMovement(new Vec3(v.x * 0.8D, v.y, v.z * 0.8D));
            Vec3 ov = other.getDeltaMovement();
            other.setDeltaMovement(new Vec3(ov.x * 0.8D, ov.y, ov.z * 0.8D));
            ci.cancel();
        }
    }

    @Inject(method = {"shouldShowName", "isCustomNameVisible"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$hideNametags(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        AunConfig.HideNametagsConfig cfg = AunConfigManager.get().hideNametags;
        if (self instanceof Player) {
            if (cfg.hidePlayerNametags) {
                cir.setReturnValue(false);
            }
            return;
        }
        if (self instanceof Mob) {
            if (cfg.hideMobNametags) {
                cir.setReturnValue(false);
                return;
            }
            String list = cfg.hiddenMobsList;
            if (list != null && !list.isBlank()) {
                String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(self.getType()).getPath().toLowerCase(Locale.ROOT);
                for (String token : list.split(",")) {
                    String trimmed = token.trim().toLowerCase(Locale.ROOT).replace("minecraft:", "");
                    if (!trimmed.isEmpty() && trimmed.equals(mobId)) {
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
        }
    }

    @Unique
    private static boolean aun$isMinecart(Entity entity) {
        String path = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        return path.contains("minecart");
    }
}
