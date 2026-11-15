package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityClientMixin {

    @Unique
    private static final StackWalker AUN_STACK_WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    @Inject(method = "tickEffects", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$markOwnEffectParticlesStart(CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && (Object) this == client.player) {
            AunClientFeatures.inLocalPlayerTickEffects = true;
            if (AunClientFeatures.shouldCancelOwnEffectParticle()) {
                AunClientFeatures.inLocalPlayerTickEffects = false;
                ci.cancel();
            }
        }
    }

    @Inject(method = "tickEffects", at = @At("RETURN"), require = 0)
    private void aun$markOwnEffectParticlesEnd(CallbackInfo ci) {
        AunClientFeatures.inLocalPlayerTickEffects = false;
    }

    @Inject(method = "getItemBySlot", at = @At("RETURN"), cancellable = true, require = 0)
    private void aun$hideVisualArmorOnPlayers(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        if (!((Object) this instanceof Player player) || !AunClientFeatures.shouldHidePlayerArmorNow(player)) {
            return;
        }
        if (slot != EquipmentSlot.HEAD
                && slot != EquipmentSlot.CHEST
                && slot != EquipmentSlot.LEGS
                && slot != EquipmentSlot.FEET) {
            return;
        }
        ItemStack current = cir.getReturnValue();
        if (current == null || current.isEmpty() || current.is(Items.ELYTRA)) {
            return;
        }
        boolean calledByRenderer = AUN_STACK_WALKER.walk(stream ->
                stream.limit(10).anyMatch(frame -> {
                    String name = frame.getClassName();
                    return name.contains("client.renderer") || name.contains("InventoryScreen");
                }));
        if (calledByRenderer) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
// END OF FILE
