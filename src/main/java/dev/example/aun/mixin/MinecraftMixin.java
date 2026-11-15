package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BlockItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow public LocalPlayer player;
    @Shadow private int rightClickDelay;

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void aun$accurateBlockPlacement(CallbackInfo ci) {
        if (!AunConfigManager.get().accurateBlockPlacement || this.player == null) {
            return;
        }
        boolean holdingBlock = this.player.getMainHandItem().getItem() instanceof BlockItem
                || this.player.getOffhandItem().getItem() instanceof BlockItem;
        if (holdingBlock && this.rightClickDelay > 1) {
            this.rightClickDelay = 1;
        }
    }
}
