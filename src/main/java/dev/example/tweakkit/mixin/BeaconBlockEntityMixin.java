package dev.example.tweakkit.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.example.tweakkit.config.TweakConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Feature 2 - tinted glass hides the beacon beam while effects keep working.
 *
 * Part A (logic): tinted glass only counts as see-through for the beam calculation while
 *                 the toggle is ON; with it OFF the vanilla opacity check applies again.
 * Part B (visual): the renderer reads getBeamSections() each frame; if tinted glass is
 *                 anywhere above the beacon we return an empty list so nothing is drawn.
 *                 The internal list used for effects is untouched.
 */
@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin {

    @Unique private long tweakkit$lastScanTime = Long.MIN_VALUE;
    @Unique private boolean tweakkit$tintedGlassAbove = false;

    @WrapOperation(
        method = "tick",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;")
    )
    private static Block tweakkit$tintedGlassOnlyTransparentWhenEnabled(BlockState state, Operation<Block> original) {
        Block block = original.call(state);
        if (block == Blocks.TINTED_GLASS && !TweakConfigManager.get().hideBeaconBeamBehindTintedGlass) {
            return Blocks.STONE; // not a BeaconBeamBlock -> vanilla light-block check blocks the beam
        }
        return block;
    }

    @Inject(method = "getBeamSections", at = @At("HEAD"), cancellable = true)
    private void tweakkit$hideBeam(CallbackInfoReturnable<List<BeaconBlockEntity.BeaconBeamSection>> cir) {
        if (!TweakConfigManager.get().hideBeaconBeamBehindTintedGlass) {
            return;
        }
        BeaconBlockEntity self = (BeaconBlockEntity) (Object) this;
        Level level = self.getLevel();
        if (level == null) {
            return;
        }

        // Column scan cached for 10 ticks; the toggle check above is not cached.
        long now = level.getGameTime();
        if (now - this.tweakkit$lastScanTime >= 10L || now < this.tweakkit$lastScanTime) {
            this.tweakkit$lastScanTime = now;
            this.tweakkit$tintedGlassAbove = tweakkit$hasTintedGlassAbove(level, self.getBlockPos());
        }

        if (this.tweakkit$tintedGlassAbove) {
            cir.setReturnValue(List.of());
        }
    }

    @Unique
    private static boolean tweakkit$hasTintedGlassAbove(Level level, BlockPos beaconPos) {
        int topExclusive = level.getMinY() + level.getHeight();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = beaconPos.getY() + 1; y < topExclusive; y++) {
            cursor.set(beaconPos.getX(), y, beaconPos.getZ());
            if (level.getBlockState(cursor).is(Blocks.TINTED_GLASS)) {
                return true;
            }
        }
        return false;
    }
}
