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
 * Only the SERVER is changed: when the beacon scans upwards and meets tinted glass, the
 * server pretends it is a beacon block, which lets the beam pass, so the
 * beacon stays active and keeps giving its effects.
 *
 * The CLIENT is not touched at all. In vanilla, tinted glass blocks the beam on the client,
 * so no beam is drawn - exactly the "hidden beam" we want. Normal beacons are never affected.
 *
 * With the toggle OFF the server behaves like vanilla too (tinted glass shuts the beacon down).
 */
@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin {

    @WrapOperation(
        method = "tick",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;"),
        require = 0
    )
    private static Block tweakkit$serverTreatsTintedGlassAsStained(BlockState state, Operation<Block> original,
                                                                   @Local(argsOnly = true) Level level) {
        Block block = original.call(state);
        if (block == Blocks.TINTED_GLASS
            && !level.isClientSide()
            && TweakConfigManager.get().hideBeaconBeamBehindTintedGlass) {
            return Blocks.BEACON;
        }
        return block;
    }
}
