package dev.example.tweakkit.mixin;

import net.minecraft.world.level.block.TintedGlassBlock;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Intentionally empty. The beacon rule now lives entirely in BeaconBlockEntityMixin
 * (server side only). This file is kept so the mixin list stays valid; it does nothing.
 */
@Mixin(TintedGlassBlock.class)
public abstract class TintedGlassBlockMixin {
}
