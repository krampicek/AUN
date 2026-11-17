package dev.example.aun.client;

import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public class AunConfigScreen extends Screen {

    private static boolean liveKindaCheaty = false;
    private static Screen lastTrackedClothScreen = null;
    private static final Set<Object> LOCKED_CHEATY_BUTTONS = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final Map<Object, Component> ORIGINAL_FIELD_NAMES = new IdentityHashMap<>();
    private static final Function<Boolean, Component> DISABLED_TEXT_SUPPLIER = b -> Component.literal("Disabled");

    private final Screen parent;

    public AunConfigScreen(Screen parent) {
        super(Component.literal("A-U-N Settings"));
        this.parent = parent;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Screen create(Screen parent) {
        AunConfig cfg = AunConfigManager.get();
        liveKindaCheaty = cfg.general != null && cfg.general.kindaCheaty;
        LOCKED_CHEATY_BUTTONS.clear();
        ORIGINAL_FIELD_NAMES.clear();

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

    public static boolean isLockedCheatyButton(Object widget) {
        return liveKindaCheaty && LOCKED_CHEATY_BUTTONS.contains(widget);
    }

    public static void tickClothConfigScreen(Screen screen) {
        if (!isClothConfigScreen(screen)) {
            lastTrackedClothScreen = null;
            LOCKED_CHEATY_BUTTONS.clear();
            ORIGINAL_FIELD_NAMES.clear();
            return;
        }
        if (screen != lastTrackedClothScreen) {
            lastTrackedClothScreen = screen;
            AunConfig cfg = AunConfigManager.get();
            liveKindaCheaty = cfg.general != null && cfg.general.kindaCheaty;
            LOCKED_CHEATY_BUTTONS.clear();
            ORIGINAL_FIELD_NAMES.clear();
        }

        AbstractSelectionList<?> listWidget = findListWidget(screen);
        if (listWidget != null) {
            Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            scanEntriesRecursive(listWidget.children(), visited, 0);
        }
    }

    private static void scanEntriesRecursive(Object node, Set<Object> visited, int depth) {
        if (node == null || depth > 6 || !visited.add(node)) return;

        if (node instanceof Collection<?> col) {
            for (Object item : col) {
                scanEntriesRecursive(item, visited, depth + 1);
            }
            return;
        }

        String id = getEntryIdentifier(node);
        if (!id.isEmpty()) {
            if (id.contains("kindacheaty") || id.contains("kinda cheaty")) {
                Object val = getEntryValue(node);
                if (val instanceof Boolean b) {
                    liveKindaCheaty = b;
                }
            } else if (id.contains("autototem")
                    || id.contains("auto totem")
                    || id.contains("closecall")
                    || id.contains("auto save your stuff")
                    || id.contains("it was close")
                    || id.contains("itwasclose")) {
                applyCheatyLockState(node, liveKindaCheaty);
            }
        }

        Object val = getEntryValue(node);
        if (val instanceof Collection<?> subCol) {
            scanEntriesRecursive(subCol, visited, depth + 1);
        }
        if (node instanceof ContainerEventHandler ceh) {
            scanEntriesRecursive(ceh.children(), visited, depth + 1);
        }
    }

    private static String getEntryIdentifier(Object entry) {
        try {
            Method m = entry.getClass().getMethod("getFieldName");
            Object res = m.invoke(entry);
            if (res instanceof Component comp) {
                return (comp.toString() + " " + comp.getString()).toLowerCase(Locale.ROOT);
            }
        } catch (Throwable ignored) {}
        return "";
    }

    private static Object getEntryValue(Object entry) {
        try {
            Method m = entry.getClass().getMethod("getValue");
            return m.invoke(entry);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void applyCheatyLockState(Object entry, boolean lock) {
        // 1. Toggle Cloth Config's built-in editable flag (locks buttons and grays out row)
        try {
            Method setEditable = entry.getClass().getMethod("setEditable", boolean.class);
            setEditable.invoke(entry, !lock);
        } catch (Throwable ignored) {}

        // 2. Gray out the option label text when locked, and restore normal text when unlocked
        Class<?> cls = entry.getClass();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                try {
                    if (Component.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Object cur = f.get(entry);
                        if (cur instanceof Component comp) {
                            ORIGINAL_FIELD_NAMES.putIfAbsent(entry, comp);
                            Component orig = ORIGINAL_FIELD_NAMES.get(entry);
                            if (lock) {
                                String clean = orig.getString().replaceAll("§.", "");
                                f.set(entry, Component.literal("§7" + clean));
                            } else {
                                f.set(entry, orig);
                            }
                        }
                    } else if (Function.class.isAssignableFrom(f.getType())) {
                        // Override BooleanListEntry's yesNoTextSupplier so it displays "Disabled"
                        f.setAccessible(true);
                        f.set(entry, lock ? DISABLED_TEXT_SUPPLIER : null);
                    } else if (Button.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Object bObj = f.get(entry);
                        if (bObj instanceof Button btn) {
                            String txt = btn.getMessage().getString();
                            if (!txt.equalsIgnoreCase("Reset")) {
                                if (lock) {
                                    LOCKED_CHEATY_BUTTONS.add(btn);
                                    btn.active = false;
                                    btn.setMessage(Component.literal("Disabled"));
                                } else {
                                    LOCKED_CHEATY_BUTTONS.remove(btn);
                                    btn.active = true;
                                }
                            } else if (lock) {
                                btn.active = false;
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }
            cls = cls.getSuperclass();
        }
    }

    private static AbstractSelectionList<?> findListWidget(Screen screen) {
        Class<?> cls = screen.getClass();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                if (AbstractSelectionList.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        Object v = f.get(screen);
                        if (v instanceof AbstractSelectionList<?> list) return list;
                    } catch (Throwable ignored) {}
                }
            }
            cls = cls.getSuperclass();
        }
        return null;
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
