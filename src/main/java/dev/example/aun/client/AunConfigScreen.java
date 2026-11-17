package dev.example.aun.client;

import dev.example.aun.config.AunConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.function.Supplier;

public class AunConfigScreen extends Screen {

    private final Screen parent;

    public AunConfigScreen(Screen parent) {
        super(Component.literal("A-U-N Settings"));
        this.parent = parent;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Screen create(Screen parent) {
        String[] classes = {
            "me.shedaniel.autoconfig.AutoConfigClient",
            "me.shedaniel.autoconfig.AutoConfig"
        };
        for (String clsName : classes) {
            try {
                Class<?> cls = Class.forName(clsName);
                Method m = cls.getMethod("getConfigScreen", Class.class, Screen.class);
                Supplier supplier = (Supplier) m.invoke(null, AunConfig.class, parent);
                return (Screen) supplier.get();
            } catch (Throwable ignored) {
            }
        }
        return parent;
    }

    public static boolean isClothConfigScreen(Screen screen) {
        return screen != null && screen.getClass().getName().toLowerCase(Locale.ROOT).contains("clothconfig");
    }

    public static void saveWithoutClosing(Screen clothScreen) {
        try {
            Class<?> cls = clothScreen.getClass();
            while (cls != null && cls != Object.class) {
                for (Method m : cls.getDeclaredMethods()) {
                    if (m.getName().equals("saveAll") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == boolean.class) {
                        m.setAccessible(true);
                        m.invoke(clothScreen, false);
                        break;
                    }
                }
                cls = cls.getSuperclass();
            }
        } catch (Throwable ignored) {}
        try {
            me.shedaniel.autoconfig.AutoConfig.getConfigHolder(dev.example.aun.config.AunConfig.class).save();
        } catch (Throwable ignored) {}
    }

    public static void openProfilesFolder() {
        try {
            Path folder = FabricLoader.getInstance().getGameDir().toAbsolutePath().resolve("aun_profiles");
            Files.createDirectories(folder);
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("win")) new ProcessBuilder("explorer.exe", folder.toString()).start();
            else if (os.contains("mac")) new ProcessBuilder("open", folder.toString()).start();
            else new ProcessBuilder("xdg-open", folder.toString()).start();
        } catch (Throwable ignored) {}
    }

    @Override
    protected void init() {
        Screen target = create(this.parent);
        if (target != null && target != this) {
            Minecraft.getInstance().setScreenAndShow(target);
        }
    }
}
// END OF FILE
