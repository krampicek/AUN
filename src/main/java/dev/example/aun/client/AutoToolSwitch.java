package dev.example.aun.client;

import dev.example.aun.config.AunConfigManager;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Switch tools while mining: when you start breaking a block, your selected hotbar slot jumps
 * to the tool in your hotbar that breaks that block fastest (if it is better than what you hold).
 */
public final class AutoToolSwitch {

    private AutoToolSwitch() {}

    public static void register() {
        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (!level.isClientSide() || !AunConfigManager.get().autoToolSwitch
                || player.isSpectator() || player.isCreative()) {
                return InteractionResult.PASS;
            }
            BlockState state = level.getBlockState(pos);
            Inventory inventory = player.getInventory();
            float bestSpeed = player.getMainHandItem().getDestroySpeed(state);
            int bestSlot = -1;
            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = inventory.getItem(slot);
                float speed = stack.getDestroySpeed(state);
                if (speed > bestSpeed + 0.001F) {
                    bestSpeed = speed;
                    bestSlot = slot;
                }
            }
            if (bestSlot >= 0) {
                inventory.setSelectedSlot(bestSlot);
            }
            return InteractionResult.PASS;
        });
    }
}
