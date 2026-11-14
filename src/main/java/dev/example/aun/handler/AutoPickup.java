package dev.example.aun.handler;

import dev.example.aun.config.AunConfigManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Auto pickup: items that drop because a player broke a block go straight into that
 * player's inventory (what does not fit still drops on the ground).
 *
 * Two ways to know "this drop belongs to that player", so one of them always catches it:
 *   1) BlockDropMixin tells us exactly while the game is creating the drops of a block.
 *   2) The break events note "player X broke a block at P"; items that appear near P during
 *      the same server tick are handed to X.
 */
public final class AutoPickup {

    private record Marker(Player player, BlockPos pos) {}

    private static final List<Marker> MARKERS = new ArrayList<>();
    private static Player dropBreaker;
    private static BlockPos dropPos;

    private AutoPickup() {}

    // ---- called from BlockDropMixin ----
    public static void beginDrops(Entity breaker, BlockPos pos) {
        if (breaker instanceof ServerPlayer player && AunConfigManager.get().autoPickup && !player.isCreative()) {
            dropBreaker = player;
            dropPos = pos.immutable();
        }
    }

    public static void endDrops() {
        dropBreaker = null;
        dropPos = null;
    }

    // ---- events ----
    public static void register() {
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            mark(player, pos);
            return true;
        });
        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> mark(player, pos));
        ServerTickEvents.END_SERVER_TICK.register(server -> MARKERS.clear());

        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (!(entity instanceof ItemEntity itemEntity) || !AunConfigManager.get().autoPickup) {
                return;
            }
            Player taker = null;
            if (dropBreaker != null && itemEntity.blockPosition().distSqr(dropPos) <= 9) {
                taker = dropBreaker;
            } else {
                for (Marker marker : MARKERS) {
                    if (itemEntity.blockPosition().distSqr(marker.pos()) <= 4) {
                        taker = marker.player();
                        break;
                    }
                }
            }
            if (taker == null) {
                return;
            }
            ItemStack stack = itemEntity.getItem().copy();
            taker.getInventory().add(stack);
            if (stack.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(stack);
            }
        });
    }

    private static void mark(Player player, BlockPos pos) {
        if (!player.level().isClientSide() && AunConfigManager.get().autoPickup && !player.isCreative()) {
            MARKERS.add(new Marker(player, pos.immutable()));
        }
    }
}
