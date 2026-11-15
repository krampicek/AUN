package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Unique
    private static final StackWalker AUN_QUIT_WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    @Unique private boolean aun$haltRequested = false;

    @Inject(method = "halt", at = @At("HEAD"), require = 0)
    private void aun$markHaltRequested(boolean waitForServer, CallbackInfo ci) {
        this.aun$haltRequested = true;
    }

    @ModifyVariable(method = "halt", at = @At("HEAD"), argsOnly = true, require = 0)
    private boolean aun$fastQuitBackgroundSave(boolean waitForServer) {
        if (AunConfigManager.get().fastQuit) {
            return false;
        }
        return waitForServer;
    }

    @Inject(method = "isShutdown", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$returnImmediatelyToTitleOnDisconnect(CallbackInfoReturnable<Boolean> cir) {
        if (this.aun$haltRequested && AunConfigManager.get().fastQuit) {
            boolean calledFromClientDisconnect = AUN_QUIT_WALKER.walk(stream ->
                    stream.limit(6).anyMatch(f -> f.getClassName().contains("client.Minecraft")));
            if (calledFromClientDisconnect) {
                cir.setReturnValue(true);
            }
        }
    }
}
