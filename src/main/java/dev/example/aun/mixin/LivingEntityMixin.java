package dev.example.aun.mixin;

import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigQol;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "hasCustomName", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$hideCustomName(CallbackInfoReturnable<Boolean> cir) {
        if (aun$shouldHideNametag()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isCustomNameVisible", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$hideCustomNameVisible(CallbackInfoReturnable<Boolean> cir) {
        if (aun$shouldHideNametag()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true, require = 0)
    private void aun$hideStandardName(CallbackInfoReturnable<Boolean> cir) {
        if (aun$shouldHideNametag()) {
            cir.setReturnValue(false);
        }
    }

    private boolean aun$shouldHideNametag() {
        AunConfig generalCfg = AunConfigManager.get();
        if (generalCfg == null || generalCfg.hideNametags == null) return false;

        AunConfigQol.HideNametagsConfig cfg = generalCfg.hideNametags;
        
        if (generalCfg.general != null && !generalCfg.general.modEnabled) {
            return false;
        }

        Entity self = (Entity) (Object) this;
        Minecraft mc = Minecraft.getInstance();

        if (self == mc.player) return false;

        if (self instanceof Player) {
            return cfg.hidePlayerNametags;
        }

        if (cfg.hideMobNametags) return true;

        if (self instanceof TamableAnimal tamable && tamable.isTame()) {
            if (cfg.hideTamedMobNametags) return true;
        }

        if (self.getType().getCategory() == MobCategory.MONSTER) {
            if (cfg.hideHostileMobNametags) return true;
        } else {
            if (cfg.hidePassiveMobNametags) return true;
        }

        if (cfg.hiddenMobsList != null && !cfg.hiddenMobsList.isBlank()) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(self.getType()).getPath().toLowerCase(Locale.ROOT);
            String[] hidden = cfg.hiddenMobsList.split(",");
            for (String h : hidden) {
                if (id.equals(h.trim().toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }

        return false;
    }
}
// END OF FILE
