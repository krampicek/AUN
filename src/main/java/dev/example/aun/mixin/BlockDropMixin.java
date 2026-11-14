package dev.example.aun.mixin;

import dev.example.aun.handler.AutoPickup;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tells AutoPickup "these drops come from a block that entity X is breaking". */
@Pseudo
@Mixin(targets = "net.minecraft.world.level.block.Block")
public abstract class BlockDropMixin {

    private static final String SIGNATURE =
        "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;"
            + "Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;"
            + "Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V";

    @Inject(method = SIGNATURE, at = @At("HEAD"), require = 0)
    private static void aun$beginDrops(BlockState state, Level level, BlockPos pos, BlockEntity blockEntity,
                                       Entity breaker, ItemStack tool, CallbackInfo ci) {
        AutoPickup.beginDrops(breaker, pos);
    }

    @Inject(method = SIGNATURE, at = @At("RETURN"), require = 0)
    private static void aun$endDrops(BlockState state, Level level, BlockPos pos, BlockEntity blockEntity,
                                     Entity breaker, ItemStack tool, CallbackInfo ci) {
        AutoPickup.endDrops();
    }
}
