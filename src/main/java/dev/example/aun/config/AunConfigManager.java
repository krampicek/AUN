package dev.example.aun.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class AunConfigManager {

    private static boolean registered = false;
    private static final AunConfig DISABLED_VIEW = createDisabledView();

    private AunConfigManager() {}

    public static synchronized void register() {
        if (registered) return;
        AutoConfig.register(AunConfig.class, GsonConfigSerializer::new);
        registered = true;
        ensureProfileFoldersExist();
    }

    public static void ensureProfileFoldersExist() {
        try {
            Path gameDir = FabricLoader.getInstance().getGameDir().toAbsolutePath();
            Path baseDir = gameDir.resolve("Aun_profiles");
            Path exportedDir = baseDir.resolve("aun_profiles_exported");
            Path loadableDir = baseDir.resolve("aun_profiles_loadable");
            Files.createDirectories(exportedDir);
            Files.createDirectories(loadableDir);

            Path defaultProfile = loadableDir.resolve("default_profile.json");
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("aun.json");
            if (!Files.exists(defaultProfile) && Files.exists(configPath)) {
                Files.copy(configPath, defaultProfile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Throwable ignored) {}
    }

    public static AunConfig getRealConfig() {
        if (!registered) register();
        return AutoConfig.getConfigHolder(AunConfig.class).getConfig();
    }

    public static AunConfig get() {
        AunConfig real = getRealConfig();
        if (real.general != null && !real.general.modEnabled) {
            DISABLED_VIEW.general = real.general;
            DISABLED_VIEW.profile = real.profile;
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
