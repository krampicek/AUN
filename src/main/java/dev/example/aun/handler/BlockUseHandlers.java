package dev.example.aun.handler;

import dev.example.aun.client.AunClientFeatures;
import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

public final class BlockUseHandlers {

    private BlockUseHandlers() {}

    public static void register() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (player.isSpectator()) return InteractionResult.PASS;
            AunConfig cfg = AunConfigManager.get();
            cfg.syncMirrors();
            BlockPos pos = hit.getBlockPos();
            BlockState state = level.getBlockState(pos);

            // Coral Reborn: Right-click dead coral with a Water Bottle to revive it
            if (cfg.essential.coralReborn) {
                ItemStack held = player.getItemInHand(hand);
                if (isWaterBottle(held)) {
                    BlockState revived = getRevivedCoralState(state);
                    if (revived != null) {
                        if (!level.isClientSide()) {
                            level.setBlockAndUpdate(pos, revived);
                            level.playSound(null, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                            if (!player.isCreative()) {
                                player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(Items.GLASS_BOTTLE)));
                            }
                        }
                        return InteractionResult.SUCCESS;
                    }
                }
            }

            // Prevent accidental log stripping when holding an offhand item or holding right-click continuously
            if (cfg.noAccidentalLogStripping && isUnstrippedWoodOrBamboo(state)) {
                if (hand == InteractionHand.MAIN_HAND && player.getMainHandItem().is(ItemTags.AXES)) {
                    boolean hasOffhandItem = !player.getOffhandItem().isEmpty();
                    boolean holdingRightClick = level.isClientSide() && AunClientFeatures.isHoldingRightClickContinuously();
                    if (hasOffhandItem || holdingRightClick) return InteractionResult.PASS;
                } else if (hand == InteractionHand.OFF_HAND && player.getOffhandItem().is(ItemTags.AXES)) {
                    return InteractionResult.FAIL;
                }
            }

            if (hand != InteractionHand.MAIN_HAND || player.isShiftKeyDown()) return InteractionResult.PASS;

            if (cfg.entities.reachBehindSign && state.getBlock() instanceof WallSignBlock) {
                Direction facing = state.getValue(WallSignBlock.FACING);
                return BehindHelper.open(player, level, pos.relative(facing.getOpposite()), facing);
            }
            if (cfg.autoSettings.simpleHarvest) {
                return SimpleHarvest.use(player, level, hit, state);
            }
            return InteractionResult.PASS;
        });
    }

    private static boolean isWaterBottle(ItemStack stack) {
        if (stack == null || !stack.is(Items.POTION)) return false;
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.is(Potions.WATER);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState getRevivedCoralState(BlockState deadState) {
        String path = BuiltInRegistries.BLOCK.getKey(deadState.getBlock()).getPath();
        if (!path.startsWith("dead_") || !path.contains("coral")) return null;
        String livingPath = path.substring(5);
        Block livingBlock = BuiltInRegistries.BLOCK.getValue(Identifier.withDefaultNamespace(livingPath));
        if (livingBlock == null || livingBlock == deadState.getBlock()) return null;
        BlockState target = livingBlock.defaultBlockState();
        for (Property prop : deadState.getProperties()) {
            if (target.hasProperty(prop)) {
                target = target.setValue(prop, deadState.getValue(prop));
            }
        }
        return target;
    }

    private static boolean isUnstrippedWoodOrBamboo(BlockState state) {
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (path.startsWith("stripped_")) return false;
        return state.is(BlockTags.LOGS) || path.equals("bamboo_block");
    }
}
// END OF FILE
