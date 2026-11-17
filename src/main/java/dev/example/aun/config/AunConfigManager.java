package dev.example.aun.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

public final class AunConfigManager {

    private static boolean registered = false;

    private AunConfigManager() {}

    public static synchronized void register() {
        if (registered) return;
        AutoConfig.register(AunConfig.class, GsonConfigSerializer::new);
        registered = true;
    }

    public static AunConfig get() {
        if (!registered) register();
        return AutoConfig.getConfigHolder(AunConfig.class).getConfig();
    }
}
// END OF FILE
