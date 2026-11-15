package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Unique
    private static final StackWalker AUN_CART_WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    @Shadow public abstract double distanceToSqr(Entity entity);
    @Shadow public abstract AABB getBoundingBox();
    @Shadow public abstract Level level();

    @Inject(method = "push(DDD)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$cancelOnlyMinecartOnMinecartSelfBoost(double x, double y, double z, CallbackInfo ci) {
        if (!AunConfigManager.get().mcBugs.stackedMinecartFix) return;
        Entity self = (Entity) (Object) this;
        if (!aun$isMinecart(self)) return;
        boolean pushedByAnotherMinecart = AUN_CART_WALKER.walk(stream ->
                stream.limit(6).anyMatch(f -> f.getClassName().contains("Minecart")));
        if (pushedByAnotherMinecart) {
            List<Entity> overlapping = this.level().getEntities(
                    self,
                    this.getBoundingBox().deflate(0.15D),
                    e -> aun$isMinecart(e) && this.distanceToSqr(e) < 0.25D);
            if (!overlapping.isEmpty()) {
                ci.cancel();
            }
        }
    }

    @Unique
    private static boolean aun$isMinecart(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath().contains("minecart");
    }
}
// END OF FILE
