package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Anvil screen: hides the red "Too Expensive!" text (client side). Method name varies by version. */
@Pseudo
@Mixin(targets = "net.minecraft.client.gui.screens.inventory.AnvilScreen")
public abstract class AnvilScreenMixin {

    @ModifyConstant(
        method = {"renderLabels", "extractLabels", "extractRenderState", "renderFg"},
        constant = @Constant(intValue = 40),
        require = 0
    )
    private int aun$noTooExpensiveText(int original) {
        return AunConfigManager.get().anvilNoTooExpensive ? Integer.MAX_VALUE : original;
    }
}
