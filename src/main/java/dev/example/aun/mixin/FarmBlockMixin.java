package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmBlock.class)
public abstract class FarmBlockMixin {

    @Inject(method = "turnToDirt", at = @At("HEAD"), cancellable = true, require = 0)
    private static void aun$preventFarmlandTrample(
            Entity entity,
            BlockState state,
            Level level,
            BlockPos pos,
            CallbackInfo ci) {
        if (entity == null) {
            return;
        }
        if (entity instanceof Player player) {
            if (!AunConfigManager.get().noCropTrampleFeatherFalling) {
                return;
            }
            ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
            ItemEnchantments enchantments = boots.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            for (Holder<Enchantment> holder : enchantments.keySet()) {
                if (holder.is(Enchantments.FEATHER_FALLING) && enchantments.getLevel(holder) > 0) {
                    ci.cancel();
                    return;
                }
            }
        } else {
            if (AunConfigManager.get().noCropTrampleMobs) {
                ci.cancel();
            }
        }
    }
}
