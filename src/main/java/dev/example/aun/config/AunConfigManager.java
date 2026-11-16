package dev.example.aun.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class AunConfigManager {

    private static boolean registered = false;
    private static final AunConfig DISABLED_VIEW = createDisabledView();

    private AunConfigManager() {}

    public static synchronized void register() {
        if (registered) return;
        AutoConfig.register(AunConfig.class, GsonConfigSerializer::new);
        registered = true;
    }

    public static AunConfig getRealConfig() {
        if (!registered) register();
        return AutoConfig.getConfigHolder(AunConfig.class).getConfig();
    }

    public static AunConfig get() {
        AunConfig real = getRealConfig();
        if (real.general != null && !real.general.modEnabled) {
            DISABLED_VIEW.general = real.general;
            DISABLED_VIEW.modEnabled = false;
            return DISABLED_VIEW;
        }
        return real;
    }

    private static AunConfig createDisabledView() {
        AunConfig cfg = new AunConfig();
        disableAllBooleans(cfg);
        cfg.syncMirrors();
        cfg.modEnabled = false;
        return cfg;
    }

    private static void disableAllBooleans(Object obj) {
        if (obj == null) return;
        for (Field f : obj.getClass().getDeclaredFields()) {
            int m = f.getModifiers();
            if (Modifier.isStatic(m)) continue;
            try {
                f.setAccessible(true);
                if (f.getType() == boolean.class) {
                    f.setBoolean(obj, false);
                } else if (!f.getType().isPrimitive() && f.getType() != String.class && !f.getType().isEnum()) {
                    disableAllBooleans(f.get(obj));
                }
            } catch (Throwable ignored) {}
        }
    }
}
// END OF FILE
