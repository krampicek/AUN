package dev.example.aun.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class AunConfigScreen extends Screen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static AunConfig.ProfileMode initialProfileModeOnOpen = null;
    private static Button ourBrowseButton = null;
    private static Button ourSaveAndQuitButton = null;
    private static Button ourSaveButton = null;

    private static Field buttonOnPressField = null;
    private static boolean searchedOnPressField = false;

    private final Screen parent;

    public AunConfigScreen(Screen parent) {
        super(Component.literal("A-U-N Settings"));
        this.parent = parent;
    }

    public static Path getProfilesBaseDir() {
        AunConfigManager.ensureProfileFoldersExist();
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

    @SuppressWarnings("unchecked")
    public static Screen create(Screen parent) {
        AunConfigManager.ensureProfileFoldersExist();
        AunConfig real = AunConfigManager.getRealConfig();
        real.syncMirrors();
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
                Supplier<Screen> supplier = (Supplier<Screen>) m.invoke(null, AunConfig.class, parent);
                return supplier.get();
            } catch (Throwable ignored) {
            }
        }
        return parent;
    }

    public static boolean isClothConfigScreen(Screen screen) {
        return screen != null && screen.getClass().getName().toLowerCase(Locale.ROOT).contains("clothconfig");
    }

    public static void registerBottomButtons(Button browseBtn, Button saveQuitBtn, Button saveBtn) {
        ourBrowseButton = browseBtn;
        ourSaveAndQuitButton = saveQuitBtn;
        ourSaveButton = saveBtn;
    }

    public static void onButtonBuilt(Button btn) {
        if (btn == null) return;
        if (!searchedOnPressField) {
            searchedOnPressField = true;
            for (Field f : Button.class.getDeclaredFields()) {
                if (Button.OnPress.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    buttonOnPressField = f;
                    break;
                }
            }
        }
        if (buttonOnPressField != null) {
            try {
                Button.OnPress original = (Button.OnPress) buttonOnPressField.get(btn);
                Button.OnPress wrapped = clicked -> {
                    if (interceptClothButtonClick(clicked)) {
                        return;
                    }
                    if (original != null) {
                        original.onPress(clicked);
                    }
                };
                buttonOnPressField.set(btn, wrapped);
            } catch (Throwable ignored) {}
        }
    }

    public static boolean interceptClothButtonClick(Button clicked) {
        Screen cur = AunClientFeatures.currentScreen;
        if (!isClothConfigScreen(cur) || clicked == null) return false;
        String text = clicked.getMessage().getString().trim();
        if (text.equals("LINK") || text.equals("OPEN_LINK")) {
            Minecraft.getInstance().setScreenAndShow(new AunExportProfileScreen.LoadProfileScreen(cur));
            return true;
        }
        if (text.equals("EXPORT") || text.equals("OPEN_EXPORT")) {
            Minecraft.getInstance().setScreenAndShow(new AunExportProfileScreen(cur));
            return true;
        }
        if (text.equals("LANGUAGE") || text.equals("OPEN_LANGUAGE")) {
            Minecraft.getInstance().setScreenAndShow(new AunExportProfileScreen.LanguageScreen(cur));
            return true;
        }
        return false;
    }

    public static void tickClothConfigScreen(Screen screen) {
        if (!isClothConfigScreen(screen)) return;
        layoutBottomBarButtons(screen);

        AbstractSelectionList<?> listWidget = findListWidget(screen);
        if (listWidget == null) return;

        Set<Object> visited = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        inspectEntriesRecursive(screen, listWidget.children(), visited, 0);
    }

    private static void inspectEntriesRecursive(Screen screen, Object node, Set<Object> visited, int depth) {
        if (node == null || depth > 5 || !visited.add(node)) return;

        if (node instanceof Collection<?> col) {
            for (Object item : col) {
                inspectEntriesRecursive(screen, item, visited, depth + 1);
            }
            return;
        }

        // Check if this is a Cloth Config entry whose enum value was just clicked
        try {
            Method getVal = node.getClass().getMethod("getValue");
            Object val = getVal.invoke(node);
            if (val == AunConfig.LinkAction.OPEN_LINK) {
                resetEnumEntryIndex(node);
                Minecraft.getInstance().setScreenAndShow(new AunExportProfileScreen.LoadProfileScreen(screen));
                return;
            } else if (val == AunConfig.ExportAction.OPEN_EXPORT) {
                resetEnumEntryIndex(node);
                Minecraft.getInstance().setScreenAndShow(new AunExportProfileScreen(screen));
                return;
            } else if (val == AunConfig.LanguageAction.OPEN_LANGUAGE) {
                resetEnumEntryIndex(node);
                Minecraft.getInstance().setScreenAndShow(new AunExportProfileScreen.LanguageScreen(screen));
                return;
            }
            if (val instanceof AunConfig.LinkAction
                    || val instanceof AunConfig.ExportAction
                    || val instanceof AunConfig.LanguageAction) {
                hideResetButtonInActionEntry(node);
            }
        } catch (Throwable ignored) {}

        if (node instanceof ContainerEventHandler ceh) {
            for (GuiEventListener child : ceh.children()) {
                inspectEntriesRecursive(screen, child, visited, depth + 1);
            }
        }
    }

    private static void resetEnumEntryIndex(Object entry) {
        Class<?> cls = entry.getClass();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                if (f.getType() == int.class) {
                    try {
                        f.setAccessible(true);
                        f.setInt(entry, 0);
                    } catch (Throwable ignored) {}
                }
            }
            cls = cls.getSuperclass();
        }
    }

    private static void hideResetButtonInActionEntry(Object entry) {
        if (entry instanceof ContainerEventHandler ceh) {
            List<Button> btns = new ArrayList<>();
            for (GuiEventListener child : ceh.children()) {
                if (child instanceof Button b) btns.add(b);
            }
            if (btns.size() >= 2) {
                Button resetBtn = btns.get(1);
                resetBtn.visible = false;
                resetBtn.active = false;
            }
        }
    }

    public static void layoutBottomBarButtons(Screen screen) {
        int centerX = screen.width / 2;
        int bottomY = screen.height - 26;
        boolean cz = isCzechLanguage();

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

    private static boolean isCzechLanguage() {
        AunConfig cfg = AunConfigManager.getRealConfig();
        return cfg.selectedLanguage != null && cfg.selectedLanguage.equalsIgnoreCase("cs_cz");
    }

    @SuppressWarnings("unchecked")
    public static void applyModLanguage(String langCode) {
        String code = (langCode == null || langCode.isBlank()) ? "en_us" : langCode.trim().toLowerCase(Locale.ROOT);
        String resourcePath = "/assets/aun/lang/" + code + ".json";
        try (InputStream in = AunConfigScreen.class.getResourceAsStream(resourcePath)) {
            if (in == null) return;
            Map<String, String> modTranslations = GSON.fromJson(
                    new InputStreamReader(in, StandardCharsets.UTF_8),
                    new TypeToken>() {}.getType());
            if (modTranslations == null || modTranslations.isEmpty()) return;

            Language currentLang = Language.getInstance();
            Class<?> cls = currentLang.getClass();
            while (cls != null && cls != Object.class) {
                for (Field f : cls.getDeclaredFields()) {
                    if (Map.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Object existingObj = f.get(currentLang);
                        if (existingObj instanceof Map<?, ?> existingMap) {
                            Map<String, String> merged = new HashMap<>((Map<String, String>) existingMap);
                            merged.putAll(modTranslations);
                            f.set(currentLang, merged);
                            return;
                        }
                    }
                }
                cls = cls.getSuperclass();
            }
        } catch (Throwable ignored) {}
    }

    public static void applySelectedLanguageAndReopen(String langCode, Screen clothScreen) {
        AunConfig cfg = AunConfigManager.getRealConfig();
        cfg.selectedLanguage = (langCode == null || langCode.isBlank()) ? "en_us" : langCode;
        cfg.syncMirrors();
        try {
            AutoConfig.getConfigHolder(AunConfig.class).save();
        } catch (Throwable ignored) {}
        applyModLanguage(cfg.selectedLanguage);
        reopenClothScreen(clothScreen);
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

    private static void invokeClothSaveAll(Screen clothScreen) {
        if (clothScreen == null) return;
        Class<?> cls = clothScreen.getClass();
        while (cls != null && cls != Object.class) {
            for (Method m : cls.getDeclaredMethods()) {
                if (m.getName().equals("saveAll") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == boolean.class) {
                    try {
                        m.setAccessible(true);
                        m.invoke(clothScreen, false);
                    } catch (Throwable ignored) {}
                    return;
                }
            }
            cls = cls.getSuperclass();
        }
    }

    public static void saveWithoutClosing(Screen clothScreen) {
        invokeClothSaveAll(clothScreen);
        AunConfig cfg = AunConfigManager.getRealConfig();
        if (cfg.profile != null && cfg.profile.profileMode != initialProfileModeOnOpen) {
            initialProfileModeOnOpen = cfg.profile.profileMode;
            applySelectedProfileMode(cfg, clothScreen);
            return;
        }
        cfg.syncMirrors();
        try {
            AutoConfig.getConfigHolder(AunConfig.class).save();
        } catch (Throwable ignored) {}
    }

    public static void saveAndClose(Screen clothScreen) {
        invokeClothSaveAll(clothScreen);
        AunConfig cfg = AunConfigManager.getRealConfig();
        if (cfg.profile != null && cfg.profile.profileMode != initialProfileModeOnOpen) {
            initialProfileModeOnOpen = cfg.profile.profileMode;
            applySelectedProfileModeWithoutReopen(cfg);
        }
        cfg.syncMirrors();
        try {
            AutoConfig.getConfigHolder(AunConfig.class).save();
        } catch (Throwable ignored) {}
        Screen parentScreen = findParentScreen(clothScreen);
        Minecraft.getInstance().setScreenAndShow(parentScreen);
    }

    public static void exportProfileWithCustomName(Screen clothScreen, String rawName) {
        invokeClothSaveAll(clothScreen);
        AunConfig cfg = AunConfigManager.getRealConfig();
        cfg.syncMirrors();
        try {
            AutoConfig.getConfigHolder(AunConfig.class).save();
        } catch (Throwable ignored) {}

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

    private static void applySelectedProfileModeWithoutReopen(AunConfig cfg) {
        if (cfg.profile == null) return;
        if (cfg.profile.profileMode == AunConfig.ProfileMode.ALL_ON) {
            setAllBooleans(cfg, true);
        } else if (cfg.profile.profileMode == AunConfig.ProfileMode.ALL_OFF) {
            setAllBooleans(cfg, false);
        } else if (cfg.profile.profileMode == AunConfig.ProfileMode.DEFAULT) {
            boolean keepModEnabled = cfg.general.modEnabled;
            String keepToggleKey = cfg.general.toggleKey;
            String keepLang = cfg.selectedLanguage;
            AunConfig def = new AunConfig();
            copyConfigValues(def, cfg);
            cfg.general.modEnabled = keepModEnabled;
            cfg.general.toggleKey = keepToggleKey;
            cfg.selectedLanguage = keepLang;
            cfg.profile.profileMode = AunConfig.ProfileMode.DEFAULT;
        } else if (cfg.profile.profileMode == AunConfig.ProfileMode.OWN_PROFILE) {
            if (cfg.profile.customProfilePath != null && !cfg.profile.customProfilePath.isBlank()) {
                String name = cfg.profile.customProfilePath.trim();
                if (!name.toLowerCase(Locale.ROOT).endsWith(".json")) name = name + ".json";
                Path candidate = getLoadableProfilesDir().resolve(name);
                if (Files.exists(candidate)) {
                    try {
                        AunConfig loaded = GSON.fromJson(Files.readString(candidate, StandardCharsets.UTF_8), AunConfig.class);
                        if (loaded != null) {
                            copyConfigValues(loaded, cfg);
                            cfg.profile.customProfilePath = name;
                            cfg.profile.profileMode = AunConfig.ProfileMode.OWN_PROFILE;
                        }
                    } catch (Throwable ignored) {}
                }
            }
        }
    }

    public static void applySelectedProfileMode(AunConfig cfg, Screen currentScreen) {
        applySelectedProfileModeWithoutReopen(cfg);
        cfg.syncMirrors();
        try {
            AutoConfig.getConfigHolder(AunConfig.class).save();
        } catch (Throwable ignored) {}
        reopenClothScreen(currentScreen);
    }

    public static void loadProfileFromFile(Path file, String displayFileName, Screen currentScreen) {
        try {
            if (file != null && Files.exists(file)) {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                AunConfig loaded = GSON.fromJson(json, AunConfig.class);
                if (loaded != null) {
                    AunConfig current = AunConfigManager.getRealConfig();
                    String keepLang = current.selectedLanguage;
                    copyConfigValues(loaded, current);
                    current.selectedLanguage = keepLang;
                    if (current.profile != null) {
                        current.profile.customProfilePath = displayFileName;
                        current.profile.profileMode = AunConfig.ProfileMode.OWN_PROFILE;
                    }
                    initialProfileModeOnOpen = AunConfig.ProfileMode.OWN_PROFILE;
                    current.syncMirrors();
                    AutoConfig.getConfigHolder(AunConfig.class).save();
                    reopenClothScreen(currentScreen);
                    return;
                }
            }
        } catch (Throwable ignored) {}
        Minecraft.getInstance().setScreenAndShow(currentScreen);
    }

    private static Screen findParentScreen(Screen currentScreen) {
        if (currentScreen == null) return null;
        Class<?> cls = currentScreen.getClass();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                if (Screen.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        return (Screen) f.get(currentScreen);
                    } catch (Throwable ignored) {}
                }
            }
            cls = cls.getSuperclass();
        }
        return null;
    }

    private static void reopenClothScreen(Screen currentScreen) {
        Minecraft.getInstance().setScreenAndShow(create(findParentScreen(currentScreen)));
    }

    private static void setAllBooleans(Object obj, boolean val) {
        if (obj == null) return;
        for (Field f : obj.getClass().getDeclaredFields()) {
            int mod = f.getModifiers();
            if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) continue;
            try {
                f.setAccessible(true);
                if (f.getType() == boolean.class) {
                    if (obj instanceof AunConfig.GeneralSettingsConfig && f.getName().equals("modEnabled")) {
                        continue;
                    } else if (obj instanceof AunConfig.EntitiesConfig && f.getName().equals("horseInfo")) {
                        f.setBoolean(obj, false);
                    } else {
                        f.setBoolean(obj, val);
                    }
                } else if (!f.getType().isPrimitive() && f.getType() != String.class && !f.getType().isEnum()) {
                    setAllBooleans(f.get(obj), val);
                }
            } catch (Throwable ignored) {}
        }
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
