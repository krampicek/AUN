package dev.example.aun.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Feature 2 - tinted glass and beacons.
 *
 * Vanilla: tinted glass above a beacon deletes the beam AND switches the beacon off.
 * This mod: the beacon is never switched off by tinted glass (server side), and the option
 * decides what the beam looks like:
 *   ON  - the beam is hidden (the client keeps vanilla behaviour, which draws no beam)
 *   OFF - the beam stays visible (the client also lets the beam pass through tinted glass)
 */
@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin {

    @WrapOperation(
        method = "tick",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;"),
        require = 0
    )
    private static Block aun$tintedGlassAndBeacon(BlockState state, Operation<Block> original,
                                                  @Local(argsOnly = true) Level level) {
        Block block = original.call(state);
        if (block != Blocks.TINTED_GLASS) {
            return block;
        }
        if (!level.isClientSide()) {
            return Blocks.BEACON; // server: tinted glass never switches the beacon off
        }
        if (!AunConfigManager.get().hideBeaconBeamBehindTintedGlass) {
            return Blocks.BEACON; // client, option OFF: beam passes through, so it is drawn
        }
        return block; // client, option ON: vanilla blocks the beam, so nothing is drawn
    }
}
