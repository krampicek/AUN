package dev.example.aun.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class AunConfigScreen extends Screen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static AunConfig.ProfileMode initialProfileModeOnOpen = null;
    private static Button ourBrowseButton = null;
    private static Button ourSaveAndQuitButton = null;
    private static Button ourSaveButton = null;
    private static String lastAppliedLangCode = null;

    private static Method fillMethod = null;
    private static Method textMethod = null;
    private static boolean searchedDrawMethods = false;

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

    public static boolean isCzechSelected() {
        AunConfig cfg = AunConfigManager.get();
        return cfg.selectedLanguage != null && cfg.selectedLanguage.equalsIgnoreCase("cs_cz");
    }

    public static void drawRect(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
        if (!searchedDrawMethods) {
            searchedDrawMethods = true;
            try { fillMethod = g.getClass().getMethod("fill", int.class, int.class, int.class, int.class, int.class); } catch (Throwable ignored) {}
            try { textMethod = g.getClass().getMethod("text", Font.class, String.class, int.class, int.class, int.class, boolean.class); } catch (Throwable ignored) {}
        }
        if (fillMethod != null) {
            try { fillMethod.invoke(g, x1, y1, x2, y2, color); } catch (Throwable ignored) {}
        }
    }

    public static void drawCenteredTitle(GuiGraphicsExtractor graphics, Font font, String text, int x, int y) {
        if (!searchedDrawMethods) {
            searchedDrawMethods = true;
            try { textMethod = graphics.getClass().getMethod("text", Font.class, String.class, int.class, int.class, int.class, boolean.class); } catch (Throwable ignored) {}
        }
        if (textMethod != null) {
            try { textMethod.invoke(graphics, font, text, x, y, 0xFFFFFFFF, true); } catch (Throwable ignored) {}
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void applyModLanguage(String langCode) {
        String code = (langCode == null || langCode.isBlank()) ? "en_us" : langCode.trim().toLowerCase(Locale.ROOT);
        try (InputStream in = AunConfigScreen.class.getResourceAsStream("/assets/aun/lang/" + code + ".json")) {
            if (in == null) return;
            JsonObject jsonObj = GSON.fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), JsonObject.class);
            if (jsonObj == null) return;

            Language langInstance = Language.getInstance();
            if (langInstance == null) return;
            Class<?> cls = langInstance.getClass();
            while (cls != null && cls != Object.class) {
                for (Field f : cls.getDeclaredFields()) {
                    if (Map.class.isAssignableFrom(f.getType()) && !Modifier.isStatic(f.getModifiers())) {
                        f.setAccessible(true);
                        Object cur = f.get(langInstance);
                        if (cur instanceof Map map) {
                            Map mutable = new HashMap(map);
                            for (String key : jsonObj.keySet()) {
                                JsonElement el = jsonObj.get(key);
                                if (el != null && el.isJsonPrimitive()) {
                                    mutable.put(key, el.getAsString());
                                }
                            }
                            f.set(langInstance, mutable);
                            lastAppliedLangCode = code;
                            return;
                        }
                    }
                }
                cls = cls.getSuperclass();
            }
        } catch (Throwable ignored) {}
    }

    public static Path getProfilesBaseDir() {
        return FabricLoader.getInstance().getGameDir().toAbsolutePath().resolve("Aun_profiles");
    }

    public static Path getExportedProfilesDir() {
        Path dir = getProfilesBaseDir().resolve("aun_profiles_exported");
        try { Files.createDirectories(dir); } catch (Throwable ignored) {}
        return dir;
    }

    public static Path getLoadableProfilesDir() {
        Path dir = getProfilesBaseDir().resolve("aun_profiles_loadable");
        try { Files.createDirectories(dir); } catch (Throwable ignored) {}
        return dir;
    }

    public static List<String> listLoadableProfileFiles() {
        List<String> out = new ArrayList<>();
        Path dir = getLoadableProfilesDir();
        if (Files.isDirectory(dir)) {
            try (Stream<Path> s = Files.list(dir)) {
                s.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                 .map(p -> p.getFileName().toString())
                 .sorted(Comparator.naturalOrder())
                 .forEach(out::add);
            } catch (Throwable ignored) {}
        }
        if (out.isEmpty()) {
            out.add("default_profile.json");
        }
        return out;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Screen create(Screen parent) {
        AunConfig real = AunConfigManager.get();
        applyModLanguage(real.selectedLanguage);
        initialProfileModeOnOpen = real.profile != null ? real.profile.profileMode : AunConfig.ProfileMode.DEFAULT;

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
            } catch (Throwable ignored) {}
        }
        return parent;
    }

    public static boolean isClothConfigScreen(Screen screen) {
        return screen != null && screen.getClass().getName().toLowerCase(Locale.ROOT).contains("clothconfig");
    }

    public static boolean isLockedCheatyButton(Object widget) {
        return liveKindaCheaty && LOCKED_CHEATY_BUTTONS.contains(widget);
    }

    public static void registerBottomButtons(Button browseBtn, Button saveQuitBtn, Button saveBtn) {
        ourBrowseButton = browseBtn;
        ourSaveAndQuitButton = saveQuitBtn;
        ourSaveButton = saveBtn;
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

        AunConfig realCfg = AunConfigManager.get();
        if (realCfg.selectedLanguage != null && !realCfg.selectedLanguage.equalsIgnoreCase(lastAppliedLangCode)) {
            applyModLanguage(realCfg.selectedLanguage);
        }

        AbstractSelectionList<?> listWidget = findListWidget(screen);
        if (listWidget != null) {
            Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            scanEntriesRecursive(listWidget.children(), visited, 0);
        }

        layoutBottomBarButtons(screen);
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
            } else if (id.contains("autototem") || id.contains("auto totem") || id.contains("closecall")
                    || id.contains("auto save your stuff") || id.contains("it was close") || id.contains("itwasclose")) {
                applyCheatyLockState(node, liveKindaCheaty);
            } else if (id.contains("chooselanguage") || id.contains("languageaction") || id.contains("zvolit jazyk")) {
                AunConfig realCfg = AunConfigManager.get();
                boolean show = realCfg.languageSub != null && !realCfg.languageSub.syncWithMinecraft;
                toggleButtonVisibility(node, show);
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
        try {
            Method setEditable = entry.getClass().getMethod("setEditable", boolean.class);
            setEditable.invoke(entry, !lock);
        } catch (Throwable ignored) {}

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

    private static void toggleButtonVisibility(Object entry, boolean show) {
        Class<?> cls = entry.getClass();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                try {
                    if (Button.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Object bObj = f.get(entry);
                        if (bObj instanceof Button btn) {
                            btn.visible = show;
                            btn.active = show;
                        }
                    } else if (Collection.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Object cObj = f.get(entry);
                        if (cObj instanceof Collection<?> col) {
                            for (Object item : col) {
                                if (item instanceof Button btn) {
                                    btn.visible = show;
                                    btn.active = show;
                                }
                            }
                        }
                    }
                } catch (Throwable ignored) {}
            }
            cls = cls.getSuperclass();
        }
    }

    public static void layoutBottomBarButtons(Screen screen) {
        int centerX = screen.width / 2;
        int bottomY = screen.height - 26;
        boolean cz = isCzechSelected();

        if (ourBrowseButton != null) {
            ourBrowseButton.setX(centerX - 204);
            ourBrowseButton.setY(bottomY);
            ourBrowseButton.setWidth(80);
            ourBrowseButton.active = true;
            ourBrowseButton.setMessage(Component.literal(cz ? "Procházet" : "Browse"));
        }
        if (ourSaveAndQuitButton != null) {
            ourSaveAndQuitButton.setX(centerX - 12);
            ourSaveAndQuitButton.setY(bottomY);
            ourSaveAndQuitButton.setWidth(106);
            ourSaveAndQuitButton.active = true;
            ourSaveAndQuitButton.setMessage(Component.literal(cz ? "Uložit a odejít" : "Save & Quit"));
        }
        if (ourSaveButton != null) {
            ourSaveButton.setX(centerX + 100);
            ourSaveButton.setY(bottomY);
            ourSaveButton.setWidth(80);
            ourSaveButton.active = true;
            ourSaveButton.setMessage(Component.literal(cz ? "Uložit" : "Save"));
        }

        for (GuiEventListener child : screen.children()) {
            if (child instanceof Button btn
                    && btn != ourBrowseButton
                    && btn != ourSaveAndQuitButton
                    && btn != ourSaveButton
                    && btn.getY() >= screen.height - 32) {
                String msg = btn.getMessage().getString().toLowerCase(Locale.ROOT);
                if (msg.contains("cancel") || msg.contains("discard") || msg.contains("zrušit")) {
                    btn.setX(centerX - 118);
                    btn.setY(bottomY);
                    btn.setWidth(100);
                    btn.setMessage(Component.literal(cz ? "Zrušit" : "Cancel"));
                } else {
                    btn.visible = false;
                    btn.active = false;
                }
            }
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

    public static void saveAndClose(Screen clothScreen) {
        saveWithoutClosing(clothScreen);
        Screen parentScreen = null;
        Class<?> cls = clothScreen.getClass();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                if (Screen.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        parentScreen = (Screen) f.get(clothScreen);
                        break;
                    } catch (Throwable ignored) {}
                }
            }
            if (parentScreen != null) break;
            cls = cls.getSuperclass();
        }
        Minecraft.getInstance().setScreenAndShow(parentScreen);
    }

    public static void exportProfileWithCustomName(Screen clothScreen, String rawName) {
        saveWithoutClosing(clothScreen);
        AunConfig cfg = AunConfigManager.get();
        try {
            Path exportedDir = getExportedProfilesDir();
            Path loadableDir = getLoadableProfilesDir();
            String cleanName = (rawName == null || rawName.isBlank()) ? "my_profile" : rawName.trim();
            cleanName = cleanName.replaceAll("[\\\\/:*?\"<>|]", "_");
            if (!cleanName.toLowerCase(Locale.ROOT).endsWith(".json")) {
                cleanName = cleanName + ".json";
            }
            Path exportPath = exportedDir.resolve(cleanName).toAbsolutePath();
            Path loadableCopy = loadableDir.resolve(cleanName).toAbsolutePath();
            String json = GSON.toJson(cfg);
            Files.writeString(exportPath, json, StandardCharsets.UTF_8);
            Files.writeString(loadableCopy, json, StandardCharsets.UTF_8);
        } catch (Throwable ignored) {}
    }

    public static void openFolderInOS(Path folder) {
        if (folder == null) return;
        try { Files.createDirectories(folder); } catch (Throwable ignored) {}
        try {
            Object platform = Util.getPlatform();
            for (Method m : platform.getClass().getMethods()) {
                if (m.getName().equals("openPath") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == Path.class) {
                    m.invoke(platform, folder);
                    return;
                }
                if (m.getName().equals("openFile") && m.getParameterCount() == 1) {
                    m.invoke(platform, folder.toFile());
                    return;
                }
            }
        } catch (Throwable ignored) {}

        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("win")) {
                new ProcessBuilder("explorer.exe", folder.toAbsolutePath().toString()).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", folder.toAbsolutePath().toString()).start();
            } else {
                new ProcessBuilder("xdg-open", folder.toAbsolutePath().toString()).start();
            }
        } catch (Throwable ignored) {}
    }

    public static void loadProfileFromFile(Path file, String displayFileName, Screen currentScreen) {
        try {
            if (file != null && Files.exists(file)) {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                AunConfig loaded = GSON.fromJson(json, AunConfig.class);
                if (loaded != null) {
                    AunConfig current = AunConfigManager.get();
                    String keepLang = current.selectedLanguage;
                    boolean keepCheaty = current.general.kindaCheaty;
                    
                    // Manually copy properties over to live config instance to avoid replacing the root object
                    for (Field f : current.getClass().getDeclaredFields()) {
                        int mod = f.getModifiers();
                        if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) continue;
                        f.setAccessible(true);
                        Object v = f.get(loaded);
                        if (f.getType().isPrimitive() || f.getType() == String.class || f.getType().isEnum()) {
                            f.set(current, v);
                        } else {
                            copyConfigValues(v, f.get(current));
                        }
                    }

                    current.selectedLanguage = keepLang;
                    current.general.kindaCheaty = keepCheaty;
                    if (current.profile != null) {
                        current.profile.customProfilePath = displayFileName;
                        current.profile.profileMode = AunConfig.ProfileMode.OWN_PROFILE;
                    }
                    initialProfileModeOnOpen = AunConfig.ProfileMode.OWN_PROFILE;
                    current.syncMirrors();
                    me.shedaniel.autoconfig.AutoConfig.getConfigHolder(AunConfig.class).save();
                    
                    // Reopen Screen
                    Screen parentScreen = null;
                    Class<?> cls = currentScreen.getClass();
                    while (cls != null && cls != Object.class) {
                        for (Field f : cls.getDeclaredFields()) {
                            if (Screen.class.isAssignableFrom(f.getType())) {
                                f.setAccessible(true);
                                parentScreen = (Screen) f.get(currentScreen);
                                break;
                            }
                        }
                        if (parentScreen != null) break;
                        cls = cls.getSuperclass();
                    }
                    Minecraft.getInstance().setScreenAndShow(create(parentScreen));
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void copyConfigValues(Object src, Object dst) {
        if (src == null || dst == null || src.getClass() != dst.getClass()) return;
        for (Field f : src.getClass().getDeclaredFields()) {
            int mod = f.getModifiers();
            if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) continue;
            try {
                f.setAccessible(true);
                Object v = f.get(src);
                if (f.getType().isPrimitive() || f.getType() == String.class || f.getType().isEnum()) {
                    f.set(dst, v);
                } else {
                    copyConfigValues(v, f.get(dst));
                }
            } catch (Throwable ignored) {}
        }
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
