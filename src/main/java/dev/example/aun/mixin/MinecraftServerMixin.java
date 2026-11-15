package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @ModifyVariable(method = "halt", at = @At("HEAD"), argsOnly = true, require = 0)
    private boolean aun$fastQuitBackgroundSave(boolean waitForServer) {
        if (AunConfigManager.get().fastQuit) {
            return false;
        }
        return waitForServer;
    }
}
