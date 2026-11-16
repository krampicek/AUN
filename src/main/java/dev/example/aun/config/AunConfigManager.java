package dev.example.aun.config;

import com.google.gson.Gson;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/** Registers the config and hands out the live instance (edited by the A-U-N settings screen). */
public final class AunConfigManager {
    private static ConfigHolder<AunConfig> holder;
    private static final AunConfig FALLBACK = new AunConfig();
    private static final Gson GSON = new Gson();

    private AunConfigManager() {}

    public static void register() {
        holder = AutoConfig.register(AunConfig.class, GsonConfigSerializer::new);
    }

    public static AunConfig get() {
        return holder != null ? holder.getConfig() : FALLBACK;
    }

    /** Writes the current settings to config/aun.json. */
    public static void save() {
        AunConfig config = get();
        config.syncMirrors();
        if (holder != null) {
            holder.save();
        }
    }

    /** The current settings as text, so the settings screen can undo changes with "Cancel". */
    public static String snapshot() {
        return GSON.toJson(get());
    }

    /** Puts every setting back to what {@link #snapshot()} captured. */
    public static void restore(String snapshot) {
        AunConfig saved = GSON.fromJson(snapshot, AunConfig.class);
        AunConfig live = get();
        for (Field field : AunConfig.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers) || Modifier.isFinal(modifiers)) {
                continue;
            }
            try {
                field.set(live, field.get(saved));
            } catch (IllegalAccessException ignored) {
                // skip fields we cannot touch
            }
        }
        live.syncMirrors();
    }
}
