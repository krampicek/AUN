package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnchantingTableBlock.class)
public abstract class EnchantingTableBlockMixin {

    @Inject(method = "isValidBookShelf", at = @At("HEAD"), cancellable = true)
    private static void aun$allowFoliageBetweenBookshelves(
            Level level,
            BlockPos tablePos,
            BlockPos offsetPos,
            CallbackInfoReturnable<Boolean> cir) {
        if (!AunConfigManager.get().enchantThroughFoliage) {
            return;
        }
        if (!level.getBlockState(tablePos.offset(offsetPos)).is(BlockTags.ENCHANTMENT_POWER_PROVIDER)) {
            return;
        }
        BlockPos betweenPos = tablePos.offset(offsetPos.getX() / 2, offsetPos.getY(), offsetPos.getZ() / 2);
        BlockState between = level.getBlockState(betweenPos);
        if (aun$isFoliage(between)) {
            cir.setReturnValue(true);
        }
    }

    @Unique
    private static boolean aun$isFoliage(BlockState state) {
        if (state.is(BlockTags.LEAVES)
                || state.is(BlockTags.FLOWERS)
                || state.is(BlockTags.SAPLINGS)
                || state.is(BlockTags.CAVE_VINES)
                || state.is(BlockTags.REPLACEABLE_BY_TREES)) {
            return true;
        }
        var block = state.getBlock();
        return block instanceof LeavesBlock
                || block instanceof BushBlock
                || block instanceof VineBlock
                || block instanceof MultifaceBlock
                || block instanceof CarpetBlock;
    }
}
