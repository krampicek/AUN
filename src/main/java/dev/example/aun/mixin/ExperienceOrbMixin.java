package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin extends Entity {

    @Unique
    private int aun$mergedXp = 0;

    protected ExperienceOrbMixin(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Inject(method = "getValue", at = @At("RETURN"), cancellable = true)
    private void aun$addMergedValue(CallbackInfoReturnable<Integer> cir) {
        if (this.aun$mergedXp > 0) {
            cir.setReturnValue(cir.getReturnValue() + this.aun$mergedXp);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void aun$mergeNearbyOrbs(CallbackInfo ci) {
        if (this.level().isClientSide() || this.isRemoved() || !AunConfigManager.get().stackingXp) {
            return;
        }
        if (this.tickCount % 10 != 0) {
            return;
        }
        ExperienceOrb self = (ExperienceOrb) (Object) this;
        List<ExperienceOrb> nearby = this.level().getEntitiesOfClass(
                ExperienceOrb.class,
                this.getBoundingBox().inflate(2.5),
                orb -> orb != self && !orb.isRemoved());
        for (ExperienceOrb other : nearby) {
            this.aun$mergedXp += other.getValue();
            other.discard();
        }
    }
}
