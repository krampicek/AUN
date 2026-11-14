package dev.example.aun.handler;

import dev.example.aun.config.AunConfigManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * "It was close": a tool in your hand that has only 1 use left is moved from the hotbar into
 * your main inventory (if there is room), so it can no longer break while you mine.
 */
public final class CloseCallHandler {

    private CloseCallHandler() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!AunConfigManager.get().closeCall) {
                return;
            }
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                rescue(player);
            }
        });
        // Also right before a block breaks, so a fast mining burst cannot slip through between ticks.
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) ->
            level.isClientSide() || !AunConfigManager.get().closeCall || !rescue(player));
    }

    /** Returns true if the held item was moved away. */
    private static boolean rescue(Player player) {
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !held.isDamageableItem() || held.getMaxDamage() - held.getDamageValue() > 1) {
            return false;
        }
        for (int slot = 9; slot < 36; slot++) {
            if (player.getInventory().getItem(slot).isEmpty()) {
                player.getInventory().setItem(slot, held.copy());
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.displayClientMessage(
                    Component.literal("That tool was about to break - it was moved to your inventory."), true);
                return true;
            }
        }
        return false;
    }
}
