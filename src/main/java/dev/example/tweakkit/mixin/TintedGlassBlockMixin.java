package dev.example.tweakkit.mixin;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BeaconBeamBlock;
import net.minecraft.world.level.block.TintedGlassBlock;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Feature 2 (part 1 - keep the effects alive).
 * Vanilla tinted glass has light-block 15, which makes the beacon tick discard its beam
 * sections and stop granting effects. Implementing BeaconBeamBlock (Mojang's name for
 * Yarn's "Stainable") makes the beacon treat it like stained glass: the beam continues
 * and the beacon stays active. BeaconBlockEntityMixin turns this off when the toggle is off.
 */
@Mixin(TintedGlassBlock.class)
public abstract class TintedGlassBlockMixin implements BeaconBeamBlock {

    @Override
    public DyeColor getColor() {
        return DyeColor.BLACK; // never visible: the beam is hidden above tinted glass
    }
}
