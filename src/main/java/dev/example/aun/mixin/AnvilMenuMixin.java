package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Anvil: the "Too Expensive!" limit of 40 levels is lifted (server side). */
@Pseudo
@Mixin(targets = "net.minecraft.world.inventory.AnvilMenu")
public abstract class AnvilMenuMixin {

    @ModifyConstant(method = "createResult", constant = @Constant(intValue = 40), require = 0)
    private int aun$noTooExpensive(int original) {
        return AunConfigManager.get().anvilNoTooExpensive ? Integer.MAX_VALUE : original;
    }
}
