package dev.example.aun.handler;

import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Right-click-on-block features:
 *   Reach behind signs (crouch = edit the sign), Simple Harvest, and Log Stripping Fix.
 */
public final class BlockUseHandlers {

    private BlockUseHandlers() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (hand != InteractionHand.MAIN_HAND || player.isSpectator() || player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            AunConfig cfg = AunConfigManager.get();
            BlockPos pos = hit.getBlockPos();
            BlockState state = level.getBlockState(pos);

            if (cfg.noAccidentalLogStripping
                    && player.getMainHandItem().getItem() instanceof AxeItem
                    && isUnstrippedWoodOrBamboo(state)) {
                return InteractionResult.FAIL;
            }

            if (cfg.reachBehindSign && state.getBlock() instanceof WallSignBlock) {
                Direction facing = state.getValue(WallSignBlock.FACING);
                return BehindHelper.open(player, level, pos.relative(facing.getOpposite()), facing);
            }
            if (cfg.simpleHarvest) {
                return SimpleHarvest.use(player, level, hit, state);
            }
            return InteractionResult.PASS;
        });
    }

    private static boolean isUnstrippedWoodOrBamboo(BlockState state) {
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (path.startsWith("stripped_")) {
            return false;
        }
        return state.is(BlockTags.LOGS) || path.equals("bamboo_block");
    }
}
