package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class GameModeLockMixin {

    @Inject(method = "setGameMode", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$lockGamemode(GameType gameMode, CallbackInfoReturnable<Boolean> cir) {
        if (AunConfigManager.get().trueVanillaGamemodeLock) {
            ServerPlayer self = (ServerPlayer) (Object) this;
            self.sendSystemMessage(Component.literal("§c[True Vanilla] Gamemode is locked in this world."));
            cir.setReturnValue(false);
        }
    }
}
