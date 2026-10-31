package dev.example.tweakkit.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers "player X was just smashed by player Y's mace (in game tick T)". Thorns damage
 * that comes back from X onto Y in the same tick is then recognised and boosted.
 */
public final class MaceTracker {
    private record Mark(UUID attacker, long tick) {}

    private static final Map<UUID, Mark> MARKS = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> BOOSTING = ThreadLocal.withInitial(() -> false);

    private MaceTracker() {}

    public static void mark(Player victim, Player attacker, long gameTime) {
        MARKS.put(victim.getUUID(), new Mark(attacker.getUUID(), gameTime));
        if (MARKS.size() > 64) { // keep the map tiny
            MARKS.values().removeIf(m -> gameTime - m.tick() > 40L);
        }
    }

    /** True if thornsOwner was smashed by target during this tick (or the tick before). */
    public static boolean matches(Entity thornsOwner, Entity target, long gameTime) {
        if (thornsOwner == null || target == null) {
            return false;
        }
        Mark mark = MARKS.get(thornsOwner.getUUID());
        return mark != null
            && mark.attacker().equals(target.getUUID())
            && gameTime - mark.tick() <= 1L;
    }

    public static boolean isBoosting() {
        return BOOSTING.get();
    }

    public static void setBoosting(boolean value) {
        BOOSTING.set(value);
    }
}
