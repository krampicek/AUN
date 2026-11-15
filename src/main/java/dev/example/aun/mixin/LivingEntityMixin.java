package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import dev.example.aun.gamerule.AunGameRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * LivingEntity features:
 *   - Mace nerf gamerule (player vs player)
 *   - Auto Totem refill on pop (R)
 *   - Firework Rocket Elytra speed fix (I)
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    protected LivingEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "getDamageAfterMagicAbsorb", at = @At("RETURN"), cancellable = true, require = 0)
    private void aun$nerfMaceOnPlayers(DamageSource source, float amount, CallbackInfoReturnable<Float> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self instanceof Player
            && source.is(DamageTypes.MACE_SMASH)
            && source.getEntity() instanceof Player
            && self.level() instanceof ServerLevel serverLevel
            && serverLevel.getGameRules().get(AunGameRules.MACE_NERF_DAMAGE_PLAYER)) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"), require = 0)
    private void aun$autoTotemRefill(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() || !AunConfigManager.get().autoTotem) {
            return;
        }
        if (!((Object) this instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        Inventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                ItemStack replacement = stack.split(1);
                if (player.getOffhandItem().isEmpty()) {
                    player.setItemInHand(InteractionHand.OFF_HAND, replacement);
                } else if (player.getMainHandItem().isEmpty()) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, replacement);
                }
                break;
            }
        }
    }

    @Override
    public void setDeltaMovement(Vec3 newDelta) {
        if (AunConfigManager.get().rocketElytraFix) {
            Vec3 delta = this.getDeltaMovement();
            Vec3 look = this.getLookAngle();
            double ex = delta.x + look.x * 0.1D + (look.x * 1.5D - delta.x) * 0.5D;
            double ey = delta.y + look.y * 0.1D + (look.y * 1.5D - delta.y) * 0.5D;
            double ez = delta.z + look.z * 0.1D + (look.z * 1.5D - delta.z) * 0.5D;
            double dx = newDelta.x - ex;
            double dy = newDelta.y - ey;
            double dz = newDelta.z - ez;
            if (dx * dx + dy * dy + dz * dz < 1.0E-6D && newDelta.lengthSqr() < delta.lengthSqr()) {
                double boostedSpeed = delta.length() + 0.05D;
                newDelta = look.normalize().scale(boostedSpeed);
            }
        }
        super.setDeltaMovement(newDelta);
    }
}
