package dev.example.aun.client;

import dev.example.aun.config.AunConfigManager;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Feature B - cut through tall grass.
 *
 * When you attack with a weapon while your crosshair is on a plant you can walk through
 * (tall grass, ferns, flowers ...), we look for a mob behind it. If there is one within reach
 * and no solid block in between, the plant is ignored and the mob is attacked instead.
 * If there is no mob, nothing changes and you cut the plant as usual.
 */
public final class CutThroughHandler {

    private CutThroughHandler() {}

    public static void register() {
        ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
            if (!AunConfigManager.get().cutThrough || player.isSpectator() || client.level == null
                || client.gameMode == null || !isWeapon(player.getMainHandItem())) {
                return false;
            }
            if (!(client.hitResult instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK) {
                return false;
            }
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = client.level.getBlockState(pos);
            if (!state.getCollisionShape(client.level, pos).isEmpty()) {
                return false; // a solid block, not a plant
            }
            Entity target = findEntityBehind(client.level, player);
            if (target == null) {
                return false;
            }
            client.gameMode.attack(player, target);
            player.swing(InteractionHand.MAIN_HAND);
            return true; // we handled the attack
        });
    }

    private static boolean isWeapon(ItemStack stack) {
        return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)
            || stack.is(Items.MACE) || stack.is(Items.TRIDENT);
    }

    private static Entity findEntityBehind(Level level, Player player) {
        double reach = player.entityInteractionRange();
        Vec3 from = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 to = from.add(look.scale(reach));

        // Solid blocks (collision shape) stop the search; plants have none and are ignored.
        BlockHitResult solid = level.clip(
            new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double maxDistSq = solid.getType() == HitResult.Type.MISS
            ? reach * reach
            : solid.getLocation().distanceToSqr(from);

        AABB box = player.getBoundingBox().expandTowards(look.scale(reach)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
            player, from, to, box, e -> !e.isSpectator() && e.isPickable(), maxDistSq);
        return hit == null ? null : hit.getEntity();
    }
}
