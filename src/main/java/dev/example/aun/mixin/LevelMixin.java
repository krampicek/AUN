package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    @ModifyVariable(method = "explode", at = @At("HEAD"), argsOnly = true, require = 0)
    private Level.ExplosionInteraction aun$preventMobExplosionGrief(
            Level.ExplosionInteraction interaction,
            Entity source) {
        if (interaction == Level.ExplosionInteraction.MOB && source != null) {
            String path = BuiltInRegistries.ENTITY_TYPE.getKey(source.getType()).getPath();
            if (path.equals("creeper") && AunConfigManager.get().mobGriefing.antiCreeperGrief) {
                return Level.ExplosionInteraction.NONE;
            }
            if ((path.equals("fireball") || path.equals("ghast"))
                    && AunConfigManager.get().mobGriefing.antiGhastGrief) {
                return Level.ExplosionInteraction.NONE;
            }
        }
        return interaction;
    }

    @Inject(
        method = "destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",
        at = @At("HEAD"),
        cancellable = true,
        require = 0
    )
    private void aun$preventRavagerDestroyBlock(
            BlockPos pos,
            boolean dropBlock,
            Entity breaker,
            int recursionLeft,
            CallbackInfoReturnable<Boolean> cir) {
        if (breaker != null && AunConfigManager.get().mobGriefing.antiRavagerGrief) {
            String path = BuiltInRegistries.ENTITY_TYPE.getKey(breaker.getType()).getPath();
            if (path.equals("ravager")) {
                cir.setReturnValue(false);
            }
        }
    }
}
