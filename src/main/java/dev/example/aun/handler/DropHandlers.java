package dev.example.aun.handler;

import dev.example.aun.Aun;
import dev.example.aun.config.AunConfigManager;
import dev.example.aun.util.Reflect;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * "Player drops never despawn" - and vanish when you leave the world.
 *
 * Items a player drops get two tags (see PlayerMixin): a "player drop" tag and a tag for the
 * current world session. A new session starts every time a world is opened.
 *   - While the world is open the items never run out their 5 minute timer.
 *   - When the world closes, tagged items that are loaded are removed.
 *   - Tagged items from an older session (for example in chunks that were not loaded when you
 *     left) are removed the moment their chunk loads again.
 */
public final class DropHandlers {

    private DropHandlers() {}

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> Aun.newSession());
        ServerLifecycleEvents.SERVER_STOPPING.register(DropHandlers::clearDrops);

        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (!AunConfigManager.get().playerDropsNeverDespawn || !(entity instanceof ItemEntity item)) {
                return;
            }
            Set<String> tags = Reflect.tagsOf(item);
            if (tags.contains(Aun.DROP_TAG) && !tags.contains(Aun.sessionTag())) {
                item.discard();
            }
        });
    }

    private static void clearDrops(MinecraftServer server) {
        if (!AunConfigManager.get().playerDropsNeverDespawn) {
            return;
        }
        try {
            for (ServerLevel level : server.getAllLevels()) {
                List<Entity> doomed = new ArrayList<>();
                for (Entity entity : level.getAllEntities()) {
                    if (entity instanceof ItemEntity item && Reflect.tagsOf(item).contains(Aun.DROP_TAG)) {
                        doomed.add(entity);
                    }
                }
                doomed.forEach(Entity::discard);
            }
        } catch (RuntimeException e) {
            Aun.LOGGER.warn("Could not clear player drops on shutdown", e);
        }
    }
}
