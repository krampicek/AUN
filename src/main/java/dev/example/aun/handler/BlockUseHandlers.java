package dev.example.aun.handler;

import dev.example.aun.client.AunClientFeatures;
import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockUseHandlers {

    private BlockUseHandlers() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (player.isSpectator()) {
                return InteractionResult.PASS;
            }
            AunConfig cfg = AunConfigManager.get();
            BlockPos pos = hit.getBlockPos();
            BlockState state = level.getBlockState(pos);

            // AG: Prevent accidental log stripping when holding an offhand item or holding right-click continuously
            if (cfg.noAccidentalLogStripping && isUnstrippedWoodOrBamboo(state)) {
                if (hand == InteractionHand.MAIN_HAND && player.getMainHandItem().is(ItemTags.AXES)) {
                    boolean hasOffhandItem = !player.getOffhandItem().isEmpty();
                    boolean holdingRightClick = level.isClientSide() && AunClientFeatures.isHoldingRightClickContinuously();
                    if (hasOffhandItem || holdingRightClick) {
                        return InteractionResult.PASS;
                    }
                } else if (hand == InteractionHand.OFF_HAND && player.getOffhandItem().is(ItemTags.AXES)) {
                    return InteractionResult.FAIL;
                }
            }

            if (hand != InteractionHand.MAIN_HAND || player.isShiftKeyDown()) {
                return InteractionResult.PASS;
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
