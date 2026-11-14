package dev.example.aun.handler;

import dev.example.aun.config.AunConfigManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Auto pickup: items that drop because a player broke a block go straight into that
 * player's inventory (anything that does not fit still drops on the ground).
 * We remember "player X is breaking the block at P" between the BEFORE and AFTER break
 * events; items that spawn next to P in that window are handed to the player.
 */
public final class AutoPickup {

    private static Player breaker;
    private static BlockPos breakPos;

    private AutoPickup() {}

    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (!level.isClientSide() && AunConfigManager.get().autoPickup && !player.isCreative()) {
                breaker = player;
                breakPos = pos.immutable();
            }
            return true;
        });
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> clear());
        PlayerBlockBreakEvents.CANCELED.register((level, player, pos, state, blockEntity) -> clear());

        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (breaker == null || !(entity instanceof ItemEntity itemEntity)
                || itemEntity.blockPosition().distSqr(breakPos) > 4) {
                return;
            }
            ItemStack stack = itemEntity.getItem().copy();
            breaker.getInventory().add(stack);
            if (stack.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(stack);
            }
        });
    }

    private static void clear() {
        breaker = null;
        breakPos = null;
    }
}
