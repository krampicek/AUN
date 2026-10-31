package dev.example.tweakkit.mixin;

import dev.example.tweakkit.config.TweakConfigManager;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
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
 * Feature 1 - Anti-Enderman block pickup / placement, independent of where Mojang keeps
 * the Enderman goal classes.
 *
 * Every few ticks each Enderman checks its own AI goal list. While the feature is ON the
 * "take block" and "leave block" goals are removed (and remembered). When the feature is
 * switched OFF they are put back, so the vanilla behaviour returns without a restart.
 */
@Mixin(Mob.class)
public abstract class MobMixin {

    @Shadow @Final protected GoalSelector goalSelector;

    /** Removed goals as {priority, goal}, kept so they can be restored. */
    @Unique private List<Object[]> tweakkit$stash;

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void tweakkit$endermanBlockGoals(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (self.getType() != EntityType.ENDERMAN
            || self.level().isClientSide()
            || self.tickCount % 5 != 0) {
            return;
        }

        if (TweakConfigManager.get().antiEndermanPickup) {
            for (WrappedGoal wrapped : new ArrayList<>(this.goalSelector.getAvailableGoals())) {
                Goal goal = wrapped.getGoal();
                String name = goal.getClass().getSimpleName();
                if (name.equals("EndermanTakeBlockGoal") || name.equals("EndermanLeaveBlockGoal")) {
                    if (this.tweakkit$stash == null) {
                        this.tweakkit$stash = new ArrayList<>();
                    }
                    this.tweakkit$stash.add(new Object[] {wrapped.getPriority(), goal});
                    this.goalSelector.removeGoal(goal);
                }
            }
        } else if (this.tweakkit$stash != null && !this.tweakkit$stash.isEmpty()) {
            for (Object[] entry : this.tweakkit$stash) {
                this.goalSelector.addGoal((Integer) entry[0], (Goal) entry[1]);
            }
            this.tweakkit$stash.clear();
        }
    }
}
