package dev.example.aun.mixin;

import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import dev.example.aun.gamerule.AunGameRules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Unique private InteractionHand aun$totemHandBeforePop = null;

    protected LivingEntityMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$hideEntityNametags(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        AunConfig.HideNametagsConfig cfg = AunConfigManager.get().hideNametags;
        if (self instanceof Player) {
            if (cfg.hidePlayerNametags) {
                cir.setReturnValue(false);
            }
            return;
        }
        if (self instanceof Mob) {
            if (cfg.hideMobNametags) {
                cir.setReturnValue(false);
                return;
            }
            boolean isTamed = (self instanceof TamableAnimal ta && ta.isTame())
                    || (self instanceof AbstractHorse horse && horse.isTamed());
            if (isTamed && cfg.hideTamedMobNametags) {
                cir.setReturnValue(false);
                return;
            }
            boolean isHostile = self instanceof Enemy;
            if (isHostile && cfg.hideHostileMobNametags) {
                cir.setReturnValue(false);
                return;
            }
            if (!isHostile && !isTamed && cfg.hidePassiveMobNametags) {
                cir.setReturnValue(false);
                return;
            }
            String list = cfg.hiddenMobsList;
            if (list != null && !list.isBlank()) {
                String mobId = BuiltInRegistries.ENTITY_TYPE.getKey(self.getType()).getPath().toLowerCase(Locale.ROOT);
                for (String token : list.split(",")) {
                    String trimmed = token.trim().toLowerCase(Locale.ROOT).replace("minecraft:", "");
                    if (!trimmed.isEmpty() && trimmed.equals(mobId)) {
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
        }
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

    @Inject(method = "checkTotemDeathProtection", at = @At("HEAD"), require = 0)
    private void aun$recordTotemHandBeforePop(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        this.aun$totemHandBeforePop = null;
        if ((Object) this instanceof Player player) {
            if (player.getMainHandItem().is(Items.TOTEM_OF_UNDYING)) {
                this.aun$totemHandBeforePop = InteractionHand.MAIN_HAND;
            } else if (player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
                this.aun$totemHandBeforePop = InteractionHand.OFF_HAND;
            }
        }
    }

    @Inject(method = "checkTotemDeathProtection", at = @At("RETURN"), require = 0)
    private void aun$autoTotemRefill(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        InteractionHand targetHand = this.aun$totemHandBeforePop;
        this.aun$totemHandBeforePop = null;
        if (!cir.getReturnValue() || !AunConfigManager.get().autoTotem || targetHand == null) {
            return;
        }
        if (!((Object) this instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        if (!player.getItemInHand(targetHand).isEmpty()) {
            return;
        }
        Inventory inv = player.getInventory();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                ItemStack replacement = stack.split(1);
                player.setItemInHand(targetHand, replacement);
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
            // If called by FireworkRocketEntity and newDelta would slow the player down, preserve current speed without adding extra speed
            if (dx * dx + dy * dy + dz * dz < 1.0E-6D && newDelta.lengthSqr() < delta.lengthSqr()) {
                newDelta = look.normalize().scale(delta.length());
            }
        }
        super.setDeltaMovement(newDelta);
    }
}
