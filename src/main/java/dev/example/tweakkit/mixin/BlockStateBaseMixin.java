package dev.example.tweakkit.mixin;

import dev.example.tweakkit.config.TweakConfigManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Feature 1 (Enderman block pickup), version-proof.
 *
 * Endermen may only pick up blocks that are in the tag #minecraft:enderman_holdable.
 * Instead of patching the Enderman goal classes (their location changed in 26.x) we make
 * that one tag look empty while the feature is switched on. No block is "holdable",
 * so nothing can be picked up (and therefore nothing is placed either).
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {

    @Inject(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void tweakkit$noHoldableBlocks(TagKey<Block> tag, CallbackInfoReturnable<Boolean> cir) {
        if (tag.equals(BlockTags.ENDERMAN_HOLDABLE) && TweakConfigManager.get().antiEndermanPickup) {
            cir.setReturnValue(false);
        }
    }
}
