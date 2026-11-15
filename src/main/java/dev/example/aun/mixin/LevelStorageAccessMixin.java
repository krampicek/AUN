package dev.example.aun.mixin;

import dev.example.aun.Aun;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Mixin(LevelStorageSource.LevelStorageAccess.class)
public abstract class LevelStorageAccessMixin {

    @Shadow public abstract LevelStorageSource.LevelDirectory getLevelDirectory();
    @Shadow public abstract String getLevelId();

    @Inject(method = "deleteLevel", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$moveDeletedWorldToBin(CallbackInfo ci) throws IOException {
        if (!AunConfigManager.get().deletedWorldProtection) {
            return;
        }
        Path worldPath = this.getLevelDirectory().path();
        if (!Files.exists(worldPath)) {
            return;
        }
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)
                    && Desktop.getDesktop().moveToTrash(worldPath.toFile())) {
                Aun.LOGGER.info("Moved deleted world '{}' to OS Trash", this.getLevelId());
                ci.cancel();
                return;
            }
        } catch (Throwable ignored) {
        }

        Path savesDir = worldPath.getParent();
        if (savesDir != null) {
            Path binDir = savesDir.resolve("_deleted_worlds_bin");
            Files.createDirectories(binDir);
            Path target = binDir.resolve(this.getLevelId() + "_" + System.currentTimeMillis());
            Files.move(worldPath, target, StandardCopyOption.REPLACE_EXISTING);
            Aun.LOGGER.info("Moved deleted world '{}' to {}", this.getLevelId(), target);
            ci.cancel();
        }
    }
}
