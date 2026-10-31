package dev.example.tweakkit.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * Remembers "player X is currently being smashed by player Y's mace" while the game
 * processes that hit, so the thorns damage that comes back during the same hit can be
 * recognised and boosted. Thread-local, so it is safe on a server with many threads.
 */
public final class MaceTracker {
    private static final ThreadLocal<Player[]> CURRENT = new ThreadLocal<>();      // {victim, attacker}
    private static final ThreadLocal<Boolean> BOOSTING = ThreadLocal.withInitial(() -> false);

    private MaceTracker() {}

    public static void begin(Player victim, Player attacker) {
        CURRENT.set(new Player[] {victim, attacker});
    }

    public static void end() {
        CURRENT.remove();
    }

    public static boolean isVictim(Entity entity) {
        Player[] pair = CURRENT.get();
        return pair != null && pair[0] == entity;
    }

    /** True if thornsOwner is the smashed player and target is the one who swung the mace. */
    public static boolean matches(Entity thornsOwner, Entity target) {
        Player[] pair = CURRENT.get();
        return pair != null && pair[0] == thornsOwner && pair[1] == target;
    }

    public static boolean isBoosting() {
        return BOOSTING.get();
    }

    public static void setBoosting(boolean value) {
        BOOSTING.set(value);
    }
}
