package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(Block.class)
public abstract class FarmBlockMixin {

    @Inject(method = "pushEntitiesUp", at = @At("HEAD"), cancellable = true, require = 0)
    private static void aun$preventFarmlandTrample(
            BlockState fromState,
            BlockState toState,
            LevelAccessor level,
            BlockPos pos,
            CallbackInfoReturnable<BlockState> cir) {
        if (!fromState.is(Blocks.FARMLAND) || !toState.is(Blocks.DIRT) || !fromState.canSurvive(level, pos)) {
            return;
        }
        List<LivingEntity> entities = level.getEntitiesOfClass(
                LivingEntity.class, new AABB(pos).inflate(0.2D, 1.0D, 0.2D));
        for (LivingEntity entity : entities) {
            if (entity instanceof Player player) {
                if (AunConfigManager.get().noCropTrampleFeatherFalling && aun$hasFeatherFalling(player)) {
                    cir.setReturnValue(fromState);
                    return;
                }
            } else if (AunConfigManager.get().noCropTrampleMobs) {
                cir.setReturnValue(fromState);
                return;
            }
        }
    }

    @Unique
    private static boolean aun$hasFeatherFalling(Player player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        ItemEnchantments enchantments = boots.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            if (holder.is(Enchantments.FEATHER_FALLING) && enchantments.getLevel(holder) > 0) {
                return true;
            }
        }
        return false;
    }
}
