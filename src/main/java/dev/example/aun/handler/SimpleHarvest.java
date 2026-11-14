package dev.example.aun.handler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple Harvest: right-click a fully grown crop to harvest it and replant it in one go.
 *   empty hand, wooden/stone hoe -> 1x1
 *   gold/iron/copper hoe         -> 2x2 (the quarter you click toward)
 *   diamond hoe                  -> plus shape (centre + 4 neighbours)
 *   netherite hoe                -> full 3x3
 * One seed is taken from the drops (or your inventory) for replanting.
 */
final class SimpleHarvest {

    private SimpleHarvest() {}

    static InteractionResult use(Player player, Level level, BlockHitResult hit, BlockState clicked) {
        ItemStack tool = player.getMainHandItem();
        boolean hoe = tool.is(ItemTags.HOES);
        if ((!tool.isEmpty() && !hoe) || !isMature(clicked)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        int harvested = 0;
        for (BlockPos pos : area(hit, tool)) {
            BlockState state = serverLevel.getBlockState(pos);
            if (isMature(state) && harvestOne(serverLevel, player, pos, state, tool)) {
                harvested++;
            }
        }
        if (harvested > 0 && hoe) {
            int damage = tool.getDamageValue() + 1;
            if (damage >= tool.getMaxDamage()) {
                tool.shrink(1);
            } else {
                tool.setDamageValue(damage);
            }
        }
        return InteractionResult.SUCCESS;
    }

    private static boolean isMature(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        return state.getBlock() instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= 3;
    }

    private static List<BlockPos> area(BlockHitResult hit, ItemStack tool) {
        BlockPos pos = hit.getBlockPos();
        List<BlockPos> out = new ArrayList<>();
        out.add(pos);
        if (tool.isEmpty()) {
            return out;
        }
        String id = BuiltInRegistries.ITEM.getKey(tool.getItem()).getPath();
        if (id.startsWith("golden") || id.startsWith("iron") || id.startsWith("copper")) {
            int sx = hit.getLocation().x >= pos.getX() + 0.5 ? 1 : -1;
            int sz = hit.getLocation().z >= pos.getZ() + 0.5 ? 1 : -1;
            out.add(pos.offset(sx, 0, 0));
            out.add(pos.offset(0, 0, sz));
            out.add(pos.offset(sx, 0, sz));
        } else if (id.startsWith("diamond")) {
            out.add(pos.north());
            out.add(pos.south());
            out.add(pos.east());
            out.add(pos.west());
        } else if (id.startsWith("netherite")) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx != 0 || dz != 0) {
                        out.add(pos.offset(dx, 0, dz));
                    }
                }
            }
        }
        return out;
    }

    private static boolean harvestOne(ServerLevel level, Player player, BlockPos pos, BlockState state, ItemStack tool) {
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, pos, null, player, tool));
        Item seed = state.getBlock().asItem();

        boolean replanted = false;
        for (ItemStack drop : drops) {
            if (!drop.isEmpty() && drop.is(seed)) {
                drop.shrink(1);
                replanted = true;
                break;
            }
        }
        if (!replanted && !new ItemStack(seed).isEmpty()) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack slot = player.getInventory().getItem(i);
                if (!slot.isEmpty() && slot.is(seed)) {
                    slot.shrink(1);
                    replanted = true;
                    break;
                }
            }
        }
        for (ItemStack drop : drops) {
            if (!drop.isEmpty() && !player.getInventory().add(drop)) {
                Block.popResource(level, pos, drop);
            }
        }
        if (replanted) {
            level.setBlock(pos, state.getBlock().defaultBlockState(), 3);
        } else {
            level.removeBlock(pos, false);
        }
        level.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        return true;
    }
}
