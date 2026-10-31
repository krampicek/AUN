package dev.example.tweakkit.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.example.tweakkit.config.TweakConfigManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Feature 2 - tinted glass hides the beacon beam while the effects keep working.
 *
 * The beacon runs the same beam calculation on the server (effects) and on the client
 * (drawing the beam). TintedGlassBlockMixin lets the beam pass through tinted glass, and
 * here we decide per side:
 *
 *   SERVER: tinted glass is see-through -> the beacon stays active and keeps giving effects.
 *   CLIENT: tinted glass blocks the beam like in vanilla -> no beam is drawn.
 *
 * With the toggle OFF both sides behave exactly like vanilla (tinted glass blocks everything).
 */
@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin {

    @WrapOperation(
        method = "tick",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;"),
        require = 0
    )
    private static Block tweakkit$tintedGlassBeamRule(BlockState state, Operation<Block> original,
                                                      @Local(argsOnly = true) Level level) {
        Block block = original.call(state);
        if (block == Blocks.TINTED_GLASS
            && (!TweakConfigManager.get().hideBeaconBeamBehindTintedGlass || level.isClientSide())) {
            return Blocks.STONE; // not a beam-passing block -> vanilla light check blocks the beam
        }
        return block;
    }
}
