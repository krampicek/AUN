package dev.example.aun.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

/** Registers the config and hands out the live instance (edited by the Cloth Config screen). */
public final class AunConfigManager {
    private static ConfigHolder<AunConfig> holder;
    private static final AunConfig FALLBACK = new AunConfig();

    private AunConfigManager() {}

    public static void register() {
        holder = AutoConfig.register(AunConfig.class, GsonConfigSerializer::new);
    }

    public static AunConfig get() {
        return holder != null ? holder.getConfig() : FALLBACK;
    }
}
