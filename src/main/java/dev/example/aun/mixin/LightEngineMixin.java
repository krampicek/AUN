package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.lighting.LightEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LightEngine.class, BlockGetter.class})
public interface LightEngineMixin {

    @Inject(method = "getLightEmission", at = @At("RETURN"), cancellable = true, require = 0)
    private void aun$emitDynamicHeldBlockLight(BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (!AunConfigManager.get().remainingHuds.dynamicLights || pos == null) return;
        int vanilla = cir.getReturnValue();
        if (vanilla >= 15) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        for (Player p : mc.level.players()) {
            int held = AunClientFeatures.getHeldLightLevel(p);
            if (held > vanilla && (pos.equals(p.blockPosition()) || pos.equals(p.blockPosition().above()))) {
                cir.setReturnValue(held);
                return;
            }
        }
    }
}
// END OF FILE
