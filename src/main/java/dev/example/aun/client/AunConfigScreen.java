package dev.example.aun.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class AunConfigScreen extends Screen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile boolean fileDialogOpen = false;
    private static boolean wasCustomBoxFocused = false;
    private static AunConfig.ProfileMode initialProfileModeOnOpen = null;

    private static Screen trackedScreen = null;
    private static Object cachedProfileModeEntry = null;
    private static Object cachedLinkEntry = null;
    private static Object cachedExportEntry = null;
    private static EditBox cachedLinkEditBox = null;
    private static Button cachedLinkRowButton = null;
    private static Field buttonOnPressField = null;
    private static boolean searchedOnPressField = false;

    private final Screen parent;

    public AunConfigScreen(Screen parent) {
        super(Component.literal("A-U-N Settings"));
        this.parent = parent;
    }

    public static Path getProfilesBaseDir() {
        Path dir = FabricLoader.getInstance().getGameDir().toAbsolutePath().resolve("aun_profiles");
        try { Files.createDirectories(dir); } catch (Throwable ignored) {}
        return dir;
    }

    public static Path getExportedProfilesDir() {
        Path dir = getProfilesBaseDir().resolve("aun_profiles_exported");
        try { Files.createDirectories(dir); } catch (Throwable ignored) {}
        return dir;
    }

    public static Path getLinkProfilesDir() {
        Path dir = getProfilesBaseDir().resolve("aun_profiles_to_link");
        try { Files.createDirectories(dir); } catch (Throwable ignored) {}
        return dir;
    }

    @SuppressWarnings("unchecked")
    public static Screen create(Screen parent) {
        getExportedProfilesDir();
        getLinkProfilesDir();
        AunConfig real = AunConfigManager.getRealConfig();
        initialProfileModeOnOpen = real.general.profileMode;
        trackedScreen = null;
        cachedProfileModeEntry = null;
        cachedLinkEntry = null;
        cachedExportEntry = null;
        cachedLinkEditBox = null;
        cachedLinkRowButton = null;

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
        if (cfg.general.profileMode != initialProfileModeOnOpen) {
            initialProfileModeOnOpen = cfg.general.profileMode;
            if (cfg.general.profileMode == AunConfig.ProfileMode.ALL_ON
                    || cfg.general.profileMode == AunConfig.ProfileMode.ALL_OFF
                    || cfg.general.profileMode == AunConfig.ProfileMode.DEFAULT) {
                applySelectedProfileMode(cfg, clothScreen);
                return;
            }
        }
        cfg.syncMirrors();
        try {
            AutoConfig.getConfigHolder(AunConfig.class).save();
        } catch (Throwable ignored) {}
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void tickClothConfigScreen(Screen screen) {
        if (!isClothConfigScreen(screen)) {
            trackedScreen = null;
            wasCustomBoxFocused = false;
            return;
        }

        AbstractSelectionList<?> listWidget = findListWidget(screen);
        if (listWidget == null) return;
        List children = listWidget.children();

        if (trackedScreen != screen || cachedProfileModeEntry == null || (!children.contains(cachedProfileModeEntry))) {
            trackedScreen = screen;
            cachedProfileModeEntry = null;
            cachedLinkEntry = null;
            cachedExportEntry = null;
            cachedLinkEditBox = null;
            cachedLinkRowButton = null;

            for (int i = 0; i < children.size(); i++) {
                Object entry = children.get(i);
                Object val = getEntryValue(entry);
                if (val instanceof AunConfig.ProfileMode) {
                    cachedProfileModeEntry = entry;
                    if (i + 1 < children.size()) cachedLinkEntry = children.get(i + 1);
                    if (i + 2 < children.size()) cachedExportEntry = children.get(i + 2);
                    setupGeneralRowWidgets(screen, cachedLinkEntry, cachedExportEntry);
                    break;
                }
            }
        }

        if (cachedProfileModeEntry != null && children.contains(cachedProfileModeEntry)) {
            Object modeVal = getEntryValue(cachedProfileModeEntry);
            boolean isOwnProfile = (modeVal == AunConfig.ProfileMode.OWN_PROFILE);

            if (isOwnProfile) {
                int idx = children.indexOf(cachedProfileModeEntry);
                if (cachedLinkEntry != null && !children.contains(cachedLinkEntry)) {
                    children.add(Math.min(children.size(), idx + 1), cachedLinkEntry);
                }
                int linkIdx = cachedLinkEntry != null ? children.indexOf(cachedLinkEntry) : idx;
                if (cachedExportEntry != null && !children.contains(cachedExportEntry)) {
                    children.add(Math.min(children.size(), linkIdx + 1), cachedExportEntry);
                }
                if (cachedLinkRowButton != null) {
                    cachedLinkRowButton.active = true;
                    cachedLinkRowButton.setMessage(Component.literal("§bLink"));
                }
                if (cachedLinkEditBox != null) {
                    boolean focused = cachedLinkEditBox.isFocused();
                    if (focused && !wasCustomBoxFocused && !fileDialogOpen) {
                        cachedLinkEditBox.setFocused(false);
                        openProfileFileChooser(screen, cachedLinkEditBox);
                    }
                    wasCustomBoxFocused = focused;
                }
            } else {
                if (cachedLinkEntry != null) children.remove(cachedLinkEntry);
                if (cachedExportEntry != null) children.remove(cachedExportEntry);
                wasCustomBoxFocused = false;
            }
        }
    }

    private static void setupGeneralRowWidgets(Screen screen, Object linkEntry, Object exportEntry) {
        if (linkEntry instanceof ContainerEventHandler ceh) {
            for (GuiEventListener child : ceh.children()) {
                if (child instanceof EditBox eb) {
                    cachedLinkEditBox = eb;
                } else if (child instanceof Button btn) {
                    cachedLinkRowButton = btn;
                    btn.active = true;
                    btn.setMessage(Component.literal("§bLink"));
                    replaceButtonOnPress(btn, b -> openProfileFileChooser(screen, cachedLinkEditBox));
                }
            }
        }
        if (exportEntry instanceof ContainerEventHandler ceh) {
            List<Button> buttons = new ArrayList<>();
            for (GuiEventListener child : ceh.children()) {
                if (child instanceof Button btn) buttons.add(btn);
            }
            if (!buttons.isEmpty()) {
                Button mainExportBtn = buttons.get(0);
                mainExportBtn.setMessage(Component.literal("§dExport Profile"));
                replaceButtonOnPress(mainExportBtn, b -> {
                    exportOwnProfile(screen);
                    b.setMessage(Component.literal("§aExported!"));
                });
                if (buttons.size() > 1) {
                    AbstractWidget resetBtn = buttons.get(1);
                    resetBtn.visible = false;
                    resetBtn.active = false;
                }
            }
        }
    }

    private static void replaceButtonOnPress(Button btn, Button.OnPress action) {
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
            try { buttonOnPressField.set(btn, action); } catch (Throwable ignored) {}
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

    public static void openProfileFileChooser(Screen currentScreen, EditBox targetBox) {
        if (fileDialogOpen) return;
        fileDialogOpen = true;
        Path linkDir = getLinkProfilesDir();

        CompletableFuture.runAsync(() -> {
            try {
                String chosen = null;
                String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
                if (os.contains("win")) {
                    String ps = "Add-Type -AssemblyName System.Windows.Forms; " +
                            "$dummy = New-Object System.Windows.Forms.Form; " +
                            "$dummy.TopMost = $true; " +
                            "$f = New-Object System.Windows.Forms.OpenFileDialog; " +
                            "$f.Title = 'Select A-U-N Profile (.json)'; " +
                            "$f.InitialDirectory = $env:AUN_LINK_DIR; " +
                            "$f.Filter = 'JSON Profile (*.json)|*.json|All Files (*.*)|*.*'; " +
                            "if ($f.ShowDialog($dummy) -eq [System.Windows.Forms.DialogResult]::OK) { [Console]::Out.WriteLine($f.FileName) }";
                    ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-Sta", "-Command", ps);
                    pb.environment().put("AUN_LINK_DIR", linkDir.toString());
                    pb.redirectErrorStream(true);
                    Process p = pb.start();
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = r.readLine()) != null) {
                            String trimmed = line.trim();
                            if (!trimmed.isEmpty() && Files.exists(Path.of(trimmed))) {
                                chosen = trimmed;
                            }
                        }
                    }
                    p.waitFor();
                }

                if (chosen != null && !chosen.isBlank()) {
                    Path chosenPath = Path.of(chosen);
                    String fileName = chosenPath.getFileName().toString();
                    Path destInLinkDir = linkDir.resolve(fileName);
                    if (!chosenPath.toAbsolutePath().equals(destInLinkDir.toAbsolutePath())) {
                        try { Files.copy(chosenPath, destInLinkDir, StandardCopyOption.REPLACE_EXISTING); } catch (Throwable ignored) {}
                    }
                    Minecraft.getInstance().execute(() -> {
                        if (targetBox != null) {
                            targetBox.setValue(fileName);
                        }
                        AunConfig cfg = AunConfigManager.getRealConfig();
                        cfg.general.customProfilePath = fileName;
                        cfg.general.profileMode = AunConfig.ProfileMode.OWN_PROFILE;
                        initialProfileModeOnOpen = AunConfig.ProfileMode.OWN_PROFILE;
                        loadProfileFromFile(destInLinkDir, fileName, currentScreen);
                    });
                }
            } catch (Throwable ignored) {
            } finally {
                fileDialogOpen = false;
            }
        });
    }

    public static void exportOwnProfile(Screen currentScreen) {
        invokeClothSaveAll(currentScreen);
        AunConfig cfg = AunConfigManager.getRealConfig();
        cfg.syncMirrors();
        try {
            AutoConfig.getConfigHolder(AunConfig.class).save();
        } catch (Throwable ignored) {}

        CompletableFuture.runAsync(() -> {
            try {
                Path exportedDir = getExportedProfilesDir();
                String profileName = (cfg.general.customProfilePath != null && !cfg.general.customProfilePath.isBlank())
                        ? cfg.general.customProfilePath.trim()
                        : "aun_exported_profile.json";
                if (!profileName.toLowerCase(Locale.ROOT).endsWith(".json")) {
                    profileName = profileName + ".json";
                }
                Path exportPath = exportedDir.resolve(profileName).toAbsolutePath();
                Path configPath = FabricLoader.getInstance().getConfigDir().resolve("aun.json");

                if (Files.exists(configPath)) {
                    Files.copy(configPath, exportPath, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.writeString(exportPath, GSON.toJson(cfg), StandardCharsets.UTF_8);
                }

                String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
                if (os.contains("win")) {
                    new ProcessBuilder("explorer.exe", exportedDir.toAbsolutePath().toString()).start();
                } else if (os.contains("mac")) {
                    new ProcessBuilder("open", exportedDir.toAbsolutePath().toString()).start();
                } else {
                    new ProcessBuilder("xdg-open", exportedDir.toAbsolutePath().toString()).start();
                }
            } catch (Throwable ignored) {
            }
        });
    }

    public static void applySelectedProfileMode(AunConfig cfg, Screen currentScreen) {
        if (cfg.general.profileMode == AunConfig.ProfileMode.ALL_ON) {
            setAllBooleans(cfg, true);
        } else if (cfg.general.profileMode == AunConfig.ProfileMode.ALL_OFF) {
            setAllBooleans(cfg, false);
        } else if (cfg.general.profileMode == AunConfig.ProfileMode.DEFAULT) {
            boolean keepModEnabled = cfg.general.modEnabled;
            String keepToggleKey = cfg.general.toggleKey;
            AunConfig def = new AunConfig();
            copyConfigValues(def, cfg);
            cfg.general.modEnabled = keepModEnabled;
            cfg.general.toggleKey = keepToggleKey;
            cfg.general.profileMode = AunConfig.ProfileMode.DEFAULT;
        } else if (cfg.general.profileMode == AunConfig.ProfileMode.OWN_PROFILE) {
            if (cfg.general.customProfilePath != null && !cfg.general.customProfilePath.isBlank()) {
                String name = cfg.general.customProfilePath.trim();
                Path candidate = getLinkProfilesDir().resolve(name);
                if (!Files.exists(candidate)) candidate = Path.of(name);
                loadProfileFromFile(candidate, Path.of(name).getFileName().toString(), currentScreen);
                return;
            }
        }
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
                    copyConfigValues(loaded, current);
                    current.general.customProfilePath = displayFileName;
                    current.general.profileMode = AunConfig.ProfileMode.OWN_PROFILE;
                    initialProfileModeOnOpen = AunConfig.ProfileMode.OWN_PROFILE;
                    current.syncMirrors();
                    AutoConfig.getConfigHolder(AunConfig.class).save();
                    reopenClothScreen(currentScreen);
                }
            }
        } catch (Throwable ignored) {}
    }

    private static void reopenClothScreen(Screen currentScreen) {
        if (currentScreen == null) return;
        Screen parentScreen = null;
        Class<?> cls = currentScreen.getClass();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                if (Screen.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        parentScreen = (Screen) f.get(currentScreen);
                        break;
                    } catch (Throwable ignored) {}
                }
            }
            if (parentScreen != null) break;
            cls = cls.getSuperclass();
        }
        Minecraft.getInstance().setScreenAndShow(create(parentScreen));
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
