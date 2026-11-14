package dev.example.aun.mixin;

import dev.example.aun.Aun;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Per-mob tweaks, checked a few times per second on the server:
 *   Feature 1: Endermen lose their "take block" / "leave block" goals while the toggle is ON
 *              (restored when it is switched OFF).
 *   Feature D: Happy Ghasts with the Speed effect get a flying-speed bonus, like a player
 *              gets a walking-speed bonus; strength set by the percentage in the menu (100 = default).
 */
@Mixin(Mob.class)
public abstract class MobMixin {

    @Shadow @Final protected GoalSelector goalSelector;

    /** Removed Enderman goals as {priority, goal}, kept so they can be restored. */
    @Unique private List<Object[]> aun$stash;

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void aun$mobTweaks(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (self.level().isClientSide() || self.tickCount % 5 != 0) {
            return;
        }
        String type = BuiltInRegistries.ENTITY_TYPE.getKey(self.getType()).getPath();
        if (type.equals("enderman")) {
            this.aun$endermanGoals();
        } else if (type.equals("happy_ghast")) {
            this.aun$happyGhastSpeed(self);
        }
    }

    @Unique
    private void aun$endermanGoals() {
        if (AunConfigManager.get().antiEndermanPickup) {
            for (WrappedGoal wrapped : new ArrayList<>(this.goalSelector.getAvailableGoals())) {
                Goal goal = wrapped.getGoal();
                String name = goal.getClass().getSimpleName();
                if (name.equals("EndermanTakeBlockGoal") || name.equals("EndermanLeaveBlockGoal")) {
                    if (this.aun$stash == null) {
                        this.aun$stash = new ArrayList<>();
                    }
                    this.aun$stash.add(new Object[] {wrapped.getPriority(), goal});
                    this.goalSelector.removeGoal(goal);
                }
            }
        } else if (this.aun$stash != null && !this.aun$stash.isEmpty()) {
            for (Object[] entry : this.aun$stash) {
                this.goalSelector.addGoal((Integer) entry[0], (Goal) entry[1]);
            }
            this.aun$stash.clear();
        }
    }

    @Unique
    private void aun$happyGhastSpeed(Mob ghast) {
        AttributeInstance flying = ghast.getAttribute(Attributes.FLYING_SPEED);
        if (flying == null) {
            return;
        }
        MobEffectInstance speed = ghast.getEffect(MobEffects.SPEED);
        AttributeModifier current = flying.getModifier(Aun.GHAST_SPEED_ID);

        if (AunConfigManager.get().speedHappyGhast && speed != null) {
            // Default boost (100%): a player's Speed bonus (+20% per level) made 1.5x stronger,
            // i.e. Speed I -> x1.8 and Speed II -> x2.1 flying speed.
            // The menu value scales only the extra speed: 200 = double the extra, 50 = half, 0 = none.
            double defaultBonus = (1.0 + 0.2 * (speed.getAmplifier() + 1)) * 1.5 - 1.0;
            int percent = Math.max(0, Math.min(1000, AunConfigManager.get().happyGhastSpeedPercent));
            double amount = defaultBonus * percent / 100.0;
            if (current == null || current.amount() != amount) {
                flying.addOrUpdateTransientModifier(new AttributeModifier(
                    Aun.GHAST_SPEED_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        } else if (current != null) {
            flying.removeModifier(Aun.GHAST_SPEED_ID);
        }
    }
}
