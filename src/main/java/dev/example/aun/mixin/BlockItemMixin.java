package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

    @Inject(method = "getPlacementState", at = @At("RETURN"), cancellable = true, require = 0)
    private void aun$orientPlacedBlock(
            BlockPlaceContext context,
            CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
        if (state == null || !AunConfigManager.get().facingPlacement) {
            return;
        }
        int mode = AunClientFeatures.getPlaceFacingMode();
        if (mode == 0) {
            return;
        }
        Direction clickedFace = context.getClickedFace();
        Direction targetDir = (mode == 1) ? clickedFace.getOpposite() : clickedFace;

        if (state.hasProperty(BlockStateProperties.FACING)) {
            BlockState updated = state.setValue(BlockStateProperties.FACING, targetDir);
            if (updated.canSurvive(context.getLevel(), context.getClickedPos())) {
                cir.setReturnValue(updated);
            }
        } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                && targetDir.getAxis().isHorizontal()) {
            BlockState updated = state.setValue(BlockStateProperties.HORIZONTAL_FACING, targetDir);
            if (updated.canSurvive(context.getLevel(), context.getClickedPos())) {
                cir.setReturnValue(updated);
            }
        } else if (state.hasProperty(BlockStateProperties.AXIS)) {
            BlockState updated = state.setValue(BlockStateProperties.AXIS, targetDir.getAxis());
            if (updated.canSurvive(context.getLevel(), context.getClickedPos())) {
                cir.setReturnValue(updated);
            }
        }
    }
}
