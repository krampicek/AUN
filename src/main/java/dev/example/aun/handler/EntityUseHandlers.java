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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Right-click-on-entity features, all in one callback:
 *   A) Reach behind: click an item frame to open the container behind it (crouch = rotate).
 *   C) Click a villager that sits in a boat or minecart to make it get out.
 *   E) Crouch + click an armor stand to swap armor with it.
 *
 * The callback runs on both sides. The client only says "I handle this" (SUCCESS) so the
 * click is sent to the server, and the server does the real work.
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
            if (cfg.dismountOnClick && isSeatedVillager(entity)) {
                if (!level.isClientSide()) {
                    entity.stopRiding();
                }
                return InteractionResult.SUCCESS;
            }
            if (cfg.reachBehind && entity instanceof ItemFrame frame && !player.isShiftKeyDown()) {
                return openContainerBehind(player, level, frame);
            }
            return InteractionResult.PASS;
        });
    }

    // ---- A) reach behind item frames -------------------------------------------------
    private static InteractionResult openContainerBehind(Player player, Level level, ItemFrame frame) {
        BlockPos behind = frame.blockPosition().relative(frame.getDirection().getOpposite());
        BlockState state = level.getBlockState(behind);
        if (state.isAir() || state.getMenuProvider(level, behind) == null) {
            return InteractionResult.PASS; // nothing openable behind it: normal item frame behaviour
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(behind), frame.getDirection(), behind, false);
        InteractionResult result = state.useWithoutItem(level, player, hit);
        return result.consumesAction() ? result : InteractionResult.PASS;
    }

    // ---- C) dismount villagers -------------------------------------------------------
    private static boolean isSeatedVillager(Entity entity) {
        String self = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
        if (!self.equals("villager") && !self.equals("wandering_trader")) {
            return false;
        }
        Entity vehicle = entity.getVehicle();
        if (vehicle == null) {
            return false;
        }
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(vehicle.getType()).getPath();
        return id.contains("boat") || id.contains("raft") || id.contains("minecart");
    }

    // ---- E) armor stand swap ---------------------------------------------------------
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
