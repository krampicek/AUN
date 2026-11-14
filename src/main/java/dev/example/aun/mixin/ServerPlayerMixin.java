package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * Ender pearl fix (MC-306936): every player keeps a list of their thrown pearls and saves it
 * with their data; those pearls are spawned again when the player logs in. A pearl that was
 * removed from the world but not taken off that list came back to life. Before the list is
 * saved we now drop every pearl that no longer exists.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

    @Shadow @Final private Set<ThrownEnderpearl> enderPearls;

    @Inject(method = "saveEnderPearls(Lnet/minecraft/world/level/storage/ValueOutput;)V", at = @At("HEAD"), require = 0)
    private void aun$forgetRemovedPearls(ValueOutput output, CallbackInfo ci) {
        if (AunConfigManager.get().enderPearlFix) {
            this.enderPearls.removeIf(Entity::isRemoved);
        }
    }
}
