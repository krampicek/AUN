package dev.example.aun.handler;

import dev.example.aun.config.AunConfigManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Mob;

/**
 * No bats: wild bats are removed the moment they appear. Named bats and bats that were
 * made persistent (name tag, leash) are left alone.
 */
public final class SpawnHandlers {

    private SpawnHandlers() {}

    public static void register() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (!AunConfigManager.get().noBatSpawning
                || !(entity instanceof Mob mob)
                || mob.hasCustomName()
                || mob.isPersistenceRequired()) {
                return;
            }
            if (BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath().equals("bat")) {
                entity.discard();
            }
        });
    }
}
