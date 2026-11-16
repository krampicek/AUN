package dev.example.aun.mixin;

import dev.example.aun.client.AunClientFeatures;
import dev.example.aun.client.FullbrightHandler;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({LivingEntity.class, HumanoidArmorLayer.class})
public abstract class LivingEntityClientMixin {

    @Unique
    private static final StackWalker AUN_STACK_WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    @Unique
    private static final MobEffectInstance AUN_FULLBRIGHT_NV =
            new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0, false, false, false);

    @Inject(method = "hasEffect", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$fullbrightHasNightVisionForLightTextureOnly(
            Holder<MobEffect> effect,
            CallbackInfoReturnable<Boolean> cir) {
        if (!FullbrightHandler.isFullbrightActive() || effect != MobEffects.NIGHT_VISION) return;
        Minecraft client = Minecraft.getInstance();
        if (client != null && (Object) this == client.player) {
            boolean calledByLightTexture = AUN_STACK_WALKER.walk(stream ->
                    stream.limit(8).anyMatch(f -> {
                        String cn = f.getClassName();
                        return cn.contains("LightTexture") || cn.contains("Lightmap");
                    }));
            if (calledByLightTexture) {
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "getEffect", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$fullbrightGetNightVisionForLightTextureOnly(
            Holder<MobEffect> effect,
            CallbackInfoReturnable<MobEffectInstance> cir) {
        if (!FullbrightHandler.isFullbrightActive() || effect != MobEffects.NIGHT_VISION) return;
        Minecraft client = Minecraft.getInstance();
        if (client != null && (Object) this == client.player) {
            boolean calledByLightTexture = AUN_STACK_WALKER.walk(stream ->
                    stream.limit(8).anyMatch(f -> {
                        String cn = f.getClassName();
                        return cn.contains("LightTexture") || cn.contains("Lightmap");
                    }));
            if (calledByLightTexture) {
                cir.setReturnValue(AUN_FULLBRIGHT_NV);
            }
        }
    }

    @ModifyVariable(method = "*Trim*", at = @At("HEAD"), argsOnly = true, require = 0)
    private int aun$makeArmorTrimsGlowLikeGlowSquid(int packedLight) {
        if (AunConfigManager.get().essential.glowingTrims) {
            return 15728880;
        }
        return packedLight;
    }

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
