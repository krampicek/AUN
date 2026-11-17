package dev.example.aun.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import me.shedaniel.autoconfig.AutoConfig;
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

    private final Screen parent;

    public AunConfigScreen(Screen parent) {
        super(Component.literal("A-U-N Settings"));
        this.parent = parent;
    }

    public static boolean isCzechSelected() {
        AunConfig cfg = AunConfigManager.getRealConfig();
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Screen create(Screen parent) {
        AunConfigManager.ensureProfileFoldersExist();
        AunConfig real = AunConfigManager.getRealConfig();
        if (real.profile != null) {
            real.profile.linkProfile = AunConfig.LinkAction.LINK;
            real.profile.exportOwnProfile = AunConfig.ExportAction.EXPORT;
        }
        if (real.languageSub != null) {
            real.languageSub.chooseLanguage = AunConfig.LanguageAction.CHOOSE_LANGUAGE;
        }
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

    public static void tickClothConfigScreen(Screen screen) {
        if (!isClothConfigScreen(screen)) return;

        AunConfig realCfg = AunConfigManager.getRealConfig();
        if (realCfg.selectedLanguage != null && !realCfg.selectedLanguage.equalsIgnoreCase(lastAppliedLangCode)) {
            applyModLanguage(realCfg.selectedLanguage);
        }

        // Handle "Kinda Cheaty" graying out Auto Totem and It Was Close (Auto Save Your Stuff)
        if (realCfg.general != null && realCfg.general.kindaCheaty) {
            hideOrDisableCheatyEntries(screen);
        }

        // Handle conditional visibility of the "Choose Language" button when Sync with Minecraft is OFF
        if (realCfg.languageSub != null) {
            toggleLanguageButtonVisibility(screen, !realCfg.languageSub.syncWithMinecraft);
        }

        layoutBottomBarButtons(screen);

        AbstractSelectionList<?> listWidget = findListWidget(screen);
        if (listWidget != null) {
            Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
            checkEntriesForSubWindowOpen(screen, listWidget.children(), visited, 0);
        }
    }

    @SuppressWarnings("rawtypes")
    private static void hideOrDisableCheatyEntries(Screen screen) {
        AbstractSelectionList<?> listWidget = findListWidget(screen);
        if (listWidget == null) return;
        for (Object entry : listWidget.children()) {
            if (entry == null) continue;
            try {
                String str = entry.toString().toLowerCase(Locale.ROOT);
                if (str.contains("autototem") || str.contains("closecall") || str.contains("auto save")) {
                    for (Field f : entry.getClass().getDeclaredFields()) {
                        f.setAccessible(true);
                        Object val = f.get(entry);
                        if (val instanceof Button btn) {
                            btn.active = false;
                            btn.setMessage(Component.literal("§8DISABLED"));
                        } else if (val instanceof Collection col) {
                            for (Object item : col) {
                                if (item instanceof Button b2) {
                                    b2.active = false;
                                    b2.setMessage(Component.literal("§8DISABLED"));
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    @SuppressWarnings("rawtypes")
    private static void toggleLanguageButtonVisibility(Screen screen, boolean show) {
        AbstractSelectionList<?> listWidget = findListWidget(screen);
        if (listWidget == null) return;
        for (Object entry : listWidget.children()) {
            if (entry == null) continue;
            try {
                String str = entry.toString().toLowerCase(Locale.ROOT);
                if (str.contains("chooselanguage") || str.contains("languageaction")) {
                    for (Field f : entry.getClass().getDeclaredFields()) {
                        f.setAccessible(true);
                        Object val = f.get(entry);
                        if (val instanceof Button btn) {
                            btn.visible = show;
                            btn.active = show;
                        } else if (val instanceof Collection col) {
                            for (Object item : col) {
                                if (item instanceof Button b2) {
                                    b2.visible = show;
                                    b2.active = show;
                                }
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    private static void checkEntriesForSubWindowOpen(Screen screen, Object node, Set<Object> visited, int depth) {
        if (node == null || depth > 5 || !visited.add(node)) return;

        if (node instanceof Collection<?> col) {
            for (Object item : col) {
                checkEntriesForSubWindowOpen(screen, item, visited, depth + 1);
            }
            return;
        }

        Object val = getEntryValue(node);
        if (val == AunConfig.LinkAction.OPEN_LINK) {
            resetSelectionEntryIndex(node);
            invokeClothSaveAll(screen);
            AunConfigManager.getRealConfig().profile.linkProfile = AunConfig.LinkAction.LINK;
            Minecraft.getInstance().setScreenAndShow(new AunLoadProfileScreen(screen));
            return;
        }
        if (val == AunConfig.ExportAction.OPEN_EXPORT) {
            resetSelectionEntryIndex(node);
            invokeClothSaveAll(screen);
            AunConfigManager.getRealConfig().profile.exportOwnProfile = AunConfig.ExportAction.EXPORT;
            Minecraft.getInstance().setScreenAndShow(new AunExportProfileScreen(screen));
            return;
        }
        if (val == AunConfig.LanguageAction.OPEN_LANGUAGE) {
            resetSelectionEntryIndex(node);
            invokeClothSaveAll(screen);
            AunConfigManager.getRealConfig().languageSub.chooseLanguage = AunConfig.LanguageAction.CHOOSE_LANGUAGE;
            Minecraft.getInstance().setScreenAndShow(new AunLanguageScreen(screen));
            return;
        }

        if (node instanceof ContainerEventHandler ceh) {
            checkEntriesForSubWindowOpen(screen, ceh.children(), visited, depth + 1);
        }
    }

    private static Object getEntryValue(Object entry) {
        if (entry == null) return null;
        try {
            Method m = entry.getClass().getMethod("getValue");
            return m.invoke(entry);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void resetSelectionEntryIndex(Object entry) {
        if (entry == null) return;
        Class<?> cls = entry.getClass();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                if (AtomicInteger.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        AtomicInteger ai = (AtomicInteger) f.get(entry);
                        if (ai != null) ai.set(0);
                    } catch (Throwable ignored) {}
                }
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
            boolean keepCheaty = cfg.general.kindaCheaty;
            String keepToggleKey = cfg.general.toggleKey;
            String keepLang = cfg.selectedLanguage;
            AunConfig def = new AunConfig();
            copyConfigValues(def, cfg);
            cfg.general.modEnabled = keepModEnabled;
            cfg.general.kindaCheaty = keepCheaty;
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
                            String keepLang = cfg.selectedLanguage;
                            boolean keepCheaty = cfg.general.kindaCheaty;
                            copyConfigValues(loaded, cfg);
                            cfg.selectedLanguage = keepLang;
                            cfg.general.kindaCheaty = keepCheaty;
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
                    boolean keepCheaty = current.general.kindaCheaty;
                    copyConfigValues(loaded, current);
                    current.selectedLanguage = keepLang;
                    current.general.kindaCheaty = keepCheaty;
                    if (current.profile != null) {
                        current.profile.customProfilePath = displayFileName;
                        current.profile.profileMode = AunConfig.ProfileMode.OWN_PROFILE;
                    }
                    initialProfileModeOnOpen = AunConfig.ProfileMode.OWN_PROFILE;
                    current.syncMirrors();
                    AutoConfig.getConfigHolder(AunConfig.class).save();
                    reopenClothScreen(currentScreen);
                }
            }
        } catch (Throwable ignored) {}
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

    public static void reopenClothScreen(Screen currentScreen) {
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
                    if (obj instanceof AunConfig.GeneralSettingsConfig && (f.getName().equals("modEnabled") || f.getName().equals("kindaCheaty"))) {
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
