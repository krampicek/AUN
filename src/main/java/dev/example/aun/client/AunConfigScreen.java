package dev.example.aun.client;

import dev.example.aun.config.AunConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Method;
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

    public static void tickClothConfigScreen(Screen screen) {
        // No-op in baseline Cloth Config
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
