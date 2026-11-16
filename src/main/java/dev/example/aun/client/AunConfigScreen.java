package dev.example.aun.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class AunConfigScreen extends Screen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static AunConfig.ProfileMode initialProfileModeOnOpen = null;

    private static Screen trackedScreen = null;
    private static EditBox cachedProfileBox = null;
    private static Button arrowDropdownBtn = null;
    private static final List<Button> dropdownItemButtons = new ArrayList<>();
    private static boolean dropdownExpanded = false;
    private static boolean wasProfileBoxFocused = false;
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
        initialProfileModeOnOpen = real.profile != null ? real.profile.profileMode : AunConfig.ProfileMode.DEFAULT;
        trackedScreen = null;
        cachedProfileBox = null;
        dropdownExpanded = false;

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

    public static void registerOverlayWidgets(Screen screen, Button arrowBtn, List<Button> itemBtns) {
        trackedScreen = screen;
        arrowDropdownBtn = arrowBtn;
        dropdownItemButtons.clear();
        dropdownItemButtons.addAll(itemBtns);
        dropdownExpanded = false;
        cachedProfileBox = null;
    }

    public static void toggleDropdownList() {
        dropdownExpanded = !dropdownExpanded;
    }

    public static void selectProfileFromDropdown(Screen screen, int index) {
        List<String> files = listLoadableProfileFiles();
        if (index >= 0 && index < files.size()) {
            String chosenName = files.get(index);
            dropdownExpanded = false;
            if (cachedProfileBox != null) {
                cachedProfileBox.setValue(chosenName);
            }
            AunConfig cfg = AunConfigManager.getRealConfig();
            if (cfg.profile != null) {
                cfg.profile.customProfilePath = chosenName;
                cfg.profile.profileMode = AunConfig.ProfileMode.OWN_PROFILE;
                initialProfileModeOnOpen = AunConfig.ProfileMode.OWN_PROFILE;
            }
            Path file = getLoadableProfilesDir().resolve(chosenName);
            loadProfileFromFile(file, chosenName, screen);
        }
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

    public static void tickClothConfigScreen(Screen screen) {
        if (!isClothConfigScreen(screen)) {
            trackedScreen = null;
            dropdownExpanded = false;
            return;
        }

        // Layout bottom buttons with a clean 6px gap and keep Save & Quit always clickable
        for (GuiEventListener child : screen.children()) {
            if (child instanceof Button btn && btn.getY() >= screen.height - 32) {
                String msg = btn.getMessage().getString().toLowerCase(Locale.ROOT);
                if (msg.contains("cancel") || msg.contains("discard")) {
                    btn.setX(screen.width / 2 - 114);
                    btn.setWidth(110);
                } else if (msg.contains("save") && !msg.equals("save") && !msg.contains("saved")) {
                    btn.setX(screen.width / 2 + 4);
                    btn.setWidth(114);
                    btn.active = true;
                }
            }
        }

        List<EditBox> allBoxes = new ArrayList<>();
        List<Button> allButtons = new ArrayList<>();
        collectWidgetsRecursive(screen, allBoxes, allButtons);

        // Hook the Export Profile button inside the Profile sub-category
        for (int i = 0; i < allButtons.size(); i++) {
            Button b = allButtons.get(i);
            String txt = b.getMessage().getString();
            if (txt.contains("EXPORT_PROFILE")) {
                b.setMessage(Component.literal("§dExport Profile"));
                replaceButtonOnPress(b, btn -> {
                    exportOwnProfile(screen);
                    btn.setMessage(Component.literal("§aExported!"));
                });
                if (i + 1 < allButtons.size()) {
                    Button resetBtn = allButtons.get(i + 1);
                    if (resetBtn.getY() == b.getY()) {
                        resetBtn.visible = false;
                        resetBtn.active = false;
                    }
                }
            }
        }

        // Find the black EditBox inside the Profile sub-category (2nd EditBox on General tab, after Toggle Keybind)
        EditBox profileBox = null;
        for (EditBox eb : allBoxes) {
            if (eb.getY() > 36 && eb.getY() < screen.height - 36) {
                String v = eb.getValue();
                if (v != null && (v.endsWith(".json") || v.length() > 3 || allBoxes.indexOf(eb) >= 2)) {
                    profileBox = eb;
                    break;
                }
            }
        }
        cachedProfileBox = profileBox;

        if (profileBox != null && profileBox.visible) {
            boolean focused = profileBox.isFocused();
            if (focused && !wasProfileBoxFocused) {
                dropdownExpanded = true;
            }
            wasProfileBoxFocused = focused;

            if (arrowDropdownBtn != null) {
                arrowDropdownBtn.visible = true;
                arrowDropdownBtn.active = true;
                arrowDropdownBtn.setX(profileBox.getX() + profileBox.getWidth() - 18);
                arrowDropdownBtn.setY(profileBox.getY());
                arrowDropdownBtn.setWidth(18);
                arrowDropdownBtn.setMessage(Component.literal(dropdownExpanded ? "§e▲" : "§b▼"));
            }

            List<String> files = listLoadableProfileFiles();
            for (int i = 0; i < dropdownItemButtons.size(); i++) {
                Button itemBtn = dropdownItemButtons.get(i);
                if (dropdownExpanded && i < files.size()) {
                    itemBtn.visible = true;
                    itemBtn.active = true;
                    itemBtn.setX(profileBox.getX());
                    itemBtn.setY(profileBox.getY() + 19 + i * 18);
                    itemBtn.setWidth(profileBox.getWidth());
                    itemBtn.setMessage(Component.literal("§f" + files.get(i)));
                } else {
                    itemBtn.visible = false;
                    itemBtn.active = false;
                }
            }
        } else {
            wasProfileBoxFocused = false;
            if (arrowDropdownBtn != null) {
                arrowDropdownBtn.visible = false;
                arrowDropdownBtn.active = false;
            }
            for (Button itemBtn : dropdownItemButtons) {
                itemBtn.visible = false;
                itemBtn.active = false;
            }
        }
    }

    private static void collectWidgetsRecursive(GuiEventListener node, List<EditBox> boxes, List<Button> buttons) {
        if (node instanceof EditBox eb) {
            boxes.add(eb);
        } else if (node instanceof Button btn) {
            buttons.add(btn);
        } else if (node instanceof ContainerEventHandler ceh) {
            for (GuiEventListener child : ceh.children()) {
                collectWidgetsRecursive(child, boxes, buttons);
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

    public static void exportOwnProfile(Screen currentScreen) {
        invokeClothSaveAll(currentScreen);
        AunConfig cfg = AunConfigManager.getRealConfig();
        cfg.syncMirrors();
        try {
            AutoConfig.getConfigHolder(AunConfig.class).save();
        } catch (Throwable ignored) {}

        try {
            Path exportedDir = getExportedProfilesDir();
            Path loadableDir = getLoadableProfilesDir();
            String profileName = (cfg.profile != null && cfg.profile.customProfilePath != null && !cfg.profile.customProfilePath.isBlank())
                    ? cfg.profile.customProfilePath.trim()
                    : "aun_exported_profile.json";
            if (!profileName.toLowerCase(Locale.ROOT).endsWith(".json")) {
                profileName = profileName + ".json";
            }
            Path exportPath = exportedDir.resolve(profileName).toAbsolutePath();
            Path loadableCopy = loadableDir.resolve(profileName).toAbsolutePath();
            String json = GSON.toJson(cfg);
            Files.writeString(exportPath, json, StandardCharsets.UTF_8);
            Files.writeString(loadableCopy, json, StandardCharsets.UTF_8);
        } catch (Throwable ignored) {}
    }

    public static void applySelectedProfileMode(AunConfig cfg, Screen currentScreen) {
        if (cfg.profile == null) return;
        if (cfg.profile.profileMode == AunConfig.ProfileMode.ALL_ON) {
            setAllBooleans(cfg, true);
        } else if (cfg.profile.profileMode == AunConfig.ProfileMode.ALL_OFF) {
            setAllBooleans(cfg, false);
        } else if (cfg.profile.profileMode == AunConfig.ProfileMode.DEFAULT) {
            boolean keepModEnabled = cfg.general.modEnabled;
            String keepToggleKey = cfg.general.toggleKey;
            AunConfig def = new AunConfig();
            copyConfigValues(def, cfg);
            cfg.general.modEnabled = keepModEnabled;
            cfg.general.toggleKey = keepToggleKey;
            cfg.profile.profileMode = AunConfig.ProfileMode.DEFAULT;
        } else if (cfg.profile.profileMode == AunConfig.ProfileMode.OWN_PROFILE) {
            if (cfg.profile.customProfilePath != null && !cfg.profile.customProfilePath.isBlank()) {
                String name = cfg.profile.customProfilePath.trim();
                if (!name.toLowerCase(Locale.ROOT).endsWith(".json")) name = name + ".json";
                Path candidate = getLoadableProfilesDir().resolve(name);
                loadProfileFromFile(candidate, name, currentScreen);
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
