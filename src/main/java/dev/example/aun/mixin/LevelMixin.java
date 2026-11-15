package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelMixin {

    @Unique
    private static final StackWalker AUN_EXPLOSION_WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    @ModifyVariable(method = "explode", at = @At("HEAD"), argsOnly = true, require = 0)
    private Level.ExplosionInteraction aun$preventMobExplosionGrief(Level.ExplosionInteraction interaction) {
        if (interaction == Level.ExplosionInteraction.MOB) {
            boolean shouldBlock = AUN_EXPLOSION_WALKER.walk(stream ->
                    stream.limit(10).anyMatch(frame -> {
                        String cn = frame.getClassName();
                        if (cn.endsWith("Creeper") && AunConfigManager.get().mobGriefing.antiCreeperGrief) {
                            return true;
                        }
                        return (cn.endsWith("LargeFireball") || cn.endsWith("Ghast"))
                                && AunConfigManager.get().mobGriefing.antiGhastGrief;
                    }));
            if (shouldBlock) {
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
