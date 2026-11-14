package dev.example.aun.handler;

import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Right-click-on-entity features:
 *   Reach behind item frames (crouch = rotate), dismount anything from a boat/minecart,
 *   armor stand gear swap (crouch + click).
 * The client only says "I handle this" (SUCCESS) so the click is sent to the server.
 */
public final class EntityUseHandlers {

    private static final EquipmentSlot[] ARMOR_SLOTS =
        {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private EntityUseHandlers() {}

    public static void register() {
        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND || player.isSpectator()) {
                return InteractionResult.PASS;
            }
            AunConfig cfg = AunConfigManager.get();

            if (cfg.armorStandSwap && entity instanceof ArmorStand stand && player.isShiftKeyDown()) {
                return swapArmor(player, level, stand);
            }
            if (cfg.dismountOnClick && isSeatedInVehicle(entity)) {
                if (!level.isClientSide()) {
                    DismountHelper.dismountToGround(entity);
                }
                return InteractionResult.SUCCESS;
            }
            if (cfg.reachBehind && entity instanceof ItemFrame frame && !player.isShiftKeyDown()) {
                BlockPos behind = frame.blockPosition().relative(frame.getDirection().getOpposite());
                return BehindHelper.open(player, level, behind, frame.getDirection());
            }
            return InteractionResult.PASS;
        });
    }

    /** Any non-player entity that sits in a boat, raft or minecart. */
    private static boolean isSeatedInVehicle(Entity entity) {
        if (entity instanceof Player) {
            return false;
        }
        Entity vehicle = entity.getVehicle();
        if (vehicle == null) {
            return false;
        }
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType()).getPath();
        return id.contains("boat") || id.contains("raft") || id.contains("minecart");
    }

    private static InteractionResult swapArmor(Player player, Level level, ArmorStand stand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        boolean changed = false;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack mine = player.getItemBySlot(slot).copy();
            ItemStack theirs = stand.getItemBySlot(slot).copy();
            if (mine.isEmpty() && theirs.isEmpty()) {
                continue;
            }
            player.setItemSlot(slot, theirs);
            stand.setItemSlot(slot, mine);
            changed = true;
        }
        if (!changed) {
            return InteractionResult.PASS;
        }
        level.playSound(null, stand.blockPosition(), SoundEvents.ARMOR_STAND_PLACE, SoundSource.PLAYERS, 1.0F, 1.0F);
        return InteractionResult.SUCCESS;
    }
}
