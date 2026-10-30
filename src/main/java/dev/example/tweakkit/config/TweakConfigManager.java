package dev.example.tweakkit.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

/**
 * Registers the config and exposes the live instance.
 * The Cloth Config screen edits this same object and saves it on "Save",
 * so every mixin sees changes immediately - no restart needed.
 */
public final class TweakConfigManager {
    private static ConfigHolder<TweakConfig> holder;
    /** Used if a mixin runs before registration (should not normally happen). */
    private static final TweakConfig FALLBACK = new TweakConfig();

    private TweakConfigManager() {}

    public static void register() {
        holder = AutoConfig.register(TweakConfig.class, GsonConfigSerializer::new);
    }

    public static TweakConfig get() {
        return holder != null ? holder.getConfig() : FALLBACK;
    }

    /** Re-reads the JSON file from disk (e.g. after manual editing). */
    public static void reload() {
        if (holder != null) {
            holder.load();
        }
    }
}
