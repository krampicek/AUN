package dev.example.aun.util;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Tiny helpers for game methods whose names changed between versions. They look the method up
 * by name at run time, so a renamed method can never break the build.
 */
public final class Reflect {

    private Reflect() {}

    /** The scoreboard tags of an entity (called getTags() in older versions, entityTags() in newer ones). */
    @SuppressWarnings("unchecked")
    public static Set<String> tagsOf(Entity entity) {
        for (String name : new String[] {"entityTags", "getTags"}) {
            try {
                Object result = entity.getClass().getMethod(name).invoke(entity);
                if (result instanceof Set<?> set) {
                    return (Set<String>) set;
                }
            } catch (ReflectiveOperationException ignored) {
                // try the next name
            }
        }
        return Set.of();
    }

    /** Shows a short message above the hotbar of a player. */
    public static void actionBar(Player player, Component text) {
        for (Object[] candidate : new Object[][] {
            {"sendOverlayMessage", new Class<?>[] {Component.class}},
            {"displayClientMessage", new Class<?>[] {Component.class, boolean.class}},
            {"sendSystemMessage", new Class<?>[] {Component.class, boolean.class}}
        }) {
            try {
                Class<?>[] types = (Class<?>[]) candidate[1];
                Method method = player.getClass().getMethod((String) candidate[0], types);
                if (types.length == 1) {
                    method.invoke(player, text);
                } else {
                    method.invoke(player, text, true);
                }
                return;
            } catch (ReflectiveOperationException ignored) {
                // try the next name
            }
        }
    }
}
