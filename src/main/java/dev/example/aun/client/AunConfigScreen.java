package dev.example.aun.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class AunConfigScreen extends Screen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final int MAX_VISIBLE_PROFILES = 5;
    public static final int PROFILE_BOX_WIDTH = 220;
    public static final int ARROW_BTN_WIDTH = 20;

    private static AunConfig.ProfileMode initialProfileModeOnOpen = null;
    private static Button ourBrowseButton = null;
    private static Button ourSaveAndQuitButton = null;
    private static Button ourSaveButton = null;
    private static Button arrowDropdownBtn = null;
    private static final List<Button> dropdownItemButtons = new ArrayList<>();

    public static EditBox cachedProfileBox = null;
    public static Button cachedExportButton = null;
    public static int settingsProfileRowY = -1000;
    public static long lastProfileBoxRenderTimeMs = 0L;
    public static boolean dropdownExpanded = false;
    public static int dropdownScrollOffset = 0;

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
        dropdownExpanded = false;
        dropdownScrollOffset = 0;
        cachedProfileBox = null;
        cachedExportButton = null;
        settingsProfileRowY = -1000;

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

    public static void registerClothOverlayWidgets(
            Button browseBtn,
            Button saveQuitBtn,
            Button saveBtn,
            Button arrowBtn,
            List<Button> itemBtns) {
        ourBrowseButton = browseBtn;
        ourSaveAndQuitButton = saveQuitBtn;
        ourSaveButton = saveBtn;
        arrowDropdownBtn = arrowBtn;
        dropdownItemButtons.clear();
        dropdownItemButtons.addAll(itemBtns);
        dropdownExpanded = false;
        dropdownScrollOffset = 0;
        cachedProfileBox = null;
        cachedExportButton = null;
        settingsProfileRowY = -1000;
    }

    public static boolean isProfileEditBox(Object widget) {
        if (!(widget instanceof EditBox eb)) return false;
        if (!isClothConfigScreen(AunClientFeatures.currentScreen)) return false;
        if (eb == cachedProfileBox) return true;
        String val = eb.getValue();
        if (val != null && val.toLowerCase(Locale.ROOT).endsWith(".json")) {
            cachedProfileBox = eb;
            eb.setMaxLength(64);
            return true;
        }
        if (settingsProfileRowY > 0 && eb.getY() > settingsProfileRowY + 8 && eb.getY() <= settingsProfileRowY + 42) {
            cachedProfileBox = eb;
            eb.setMaxLength(64);
            return true;
        }
        return false;
    }

    public static void onWidgetSetY(Object widget, int y) {
        Screen screen = AunClientFeatures.currentScreen;
        if (!isClothConfigScreen(screen)) return;

        if (widget instanceof Button btn) {
            String txt = btn.getMessage().getString().trim();
            if (txt.equals("DEFAULT") || txt.equals("ALL_ON") || txt.equals("ALL_OFF") || txt.equals("OWN_PROFILE")) {
                settingsProfileRowY = y;
            } else if (txt.equals("EXPORT") || txt.equals("EXPORT_PROFILE")) {
                cachedExportButton = btn;
                btn.setMessage(Component.literal("EXPORT"));
                btn.visible = !dropdownExpanded;
            } else if (txt.equalsIgnoreCase("Reset")) {
                if ((cachedProfileBox != null && Math.abs(y - cachedProfileBox.getY()) <= 4)
                        || (cachedExportButton != null && Math.abs(y - cachedExportButton.getY()) <= 4)) {
                    btn.visible = false;
                    btn.active = false;
                }
            }
        }

        if (isProfileEditBox(widget)) {
            EditBox box = (EditBox) widget;
            lastProfileBoxRenderTimeMs = System.currentTimeMillis();
            boolean inBounds = y >= 34 && y <= screen.height - 42;
            if (arrowDropdownBtn != null) {
                arrowDropdownBtn.visible = inBounds;
                arrowDropdownBtn.active = inBounds;
                arrowDropdownBtn.setX(box.getX() - ARROW_BTN_WIDTH);
                arrowDropdownBtn.setY(y - 1);
                arrowDropdownBtn.setWidth(ARROW_BTN_WIDTH);
                arrowDropdownBtn.setMessage(Component.literal(dropdownExpanded ? "▲" : "▼"));
            }
            updateDropdownButtonsLayout(box.getX() - ARROW_BTN_WIDTH, y + 19, inBounds && dropdownExpanded);
        }
    }

    public static void updateDropdownButtonsLayout(int x, int topY, boolean show) {
        if (!show) {
            for (Button b : dropdownItemButtons) {
                b.visible = false;
                b.active = false;
            }
            return;
        }
        List<String> files = listLoadableProfileFiles();
        int maxScroll = Math.max(0, files.size() - MAX_VISIBLE_PROFILES);
        if (dropdownScrollOffset > maxScroll) dropdownScrollOffset = maxScroll;
        if (dropdownScrollOffset < 0) dropdownScrollOffset = 0;

        for (int i = 0; i < dropdownItemButtons.size(); i++) {
            Button b = dropdownItemButtons.get(i);
            int fileIdx = dropdownScrollOffset + i;
            if (i < MAX_VISIBLE_PROFILES && fileIdx < files.size()) {
                b.visible = true;
                b.active = true;
                b.setX(x);
                b.setY(topY + i * 18);
                b.setWidth(PROFILE_BOX_WIDTH + ARROW_BTN_WIDTH);
                b.setMessage(Component.literal(files.get(fileIdx)));
            } else {
                b.visible = false;
                b.active = false;
            }
        }
    }

    public static void tickClothConfigScreen(Screen screen) {
        if (!isClothConfigScreen(screen)) {
            dropdownExpanded = false;
            return;
        }
        layoutBottomBarButtons(screen);

        if (System.currentTimeMillis() - lastProfileBoxRenderTimeMs > 120L) {
            if (arrowDropdownBtn != null) {
                arrowDropdownBtn.visible = false;
                arrowDropdownBtn.active = false;
            }
            updateDropdownButtonsLayout(0, 0, false);
        }
    }

    public static void layoutBottomBarButtons(Screen screen) {
        int centerX = screen.width / 2;
        int bottomY = screen.height - 26;

        if (ourBrowseButton != null) {
            ourBrowseButton.setX(centerX - 204);
            ourBrowseButton.setY(bottomY);
            ourBrowseButton.setWidth(80);
            ourBrowseButton.active = true;
        }
        if (ourSaveAndQuitButton != null) {
            ourSaveAndQuitButton.setX(centerX - 12);
            ourSaveAndQuitButton.setY(bottomY);
            ourSaveAndQuitButton.setWidth(106);
            ourSaveAndQuitButton.active = true;
        }
        if (ourSaveButton != null) {
            ourSaveButton.setX(centerX + 100);
            ourSaveButton.setY(bottomY);
            ourSaveButton.setWidth(80);
            ourSaveButton.active = true;
        }

        for (GuiEventListener child : screen.children()) {
            if (child instanceof Button btn
                    && btn != ourBrowseButton
                    && btn != ourSaveAndQuitButton
                    && btn != ourSaveButton
                    && btn != arrowDropdownBtn
                    && !dropdownItemButtons.contains(btn)
                    && btn.getY() >= screen.height - 32) {
                String msg = btn.getMessage().getString().toLowerCase(Locale.ROOT);
                if (msg.contains("cancel") || msg.contains("discard")) {
                    btn.setX(centerX - 118);
                    btn.setY(bottomY);
                    btn.setWidth(100);
                    btn.setMessage(Component.literal("Cancel"));
                } else {
                    // Hide Cloth Config's built-in dark Save & Quit button so our bright one is shown instead
                    btn.visible = false;
                    btn.active = false;
                }
            }
        }
    }

    public static boolean handleCustomMouseClick(Screen screen, double mouseX, double mouseY, int button) {
        if (!isClothConfigScreen(screen) || button != 0) return false;

        // 1. Check if clicking an open dropdown item
        if (dropdownExpanded) {
            for (int i = 0; i < dropdownItemButtons.size(); i++) {
                Button itemBtn = dropdownItemButtons.get(i);
                if (itemBtn.visible && mouseX >= itemBtn.getX() && mouseX <= itemBtn.getX() + itemBtn.getWidth()
                        && mouseY >= itemBtn.getY() && mouseY <= itemBtn.getY() + itemBtn.getHeight()) {
                    selectProfileFromDropdown(screen, i);
                    return true;
                }
            }
        }

        // 2. Check if clicking the ▼ arrow button
        if (arrowDropdownBtn != null && arrowDropdownBtn.visible
                && mouseX >= arrowDropdownBtn.getX() && mouseX <= arrowDropdownBtn.getX() + arrowDropdownBtn.getWidth()
                && mouseY >= arrowDropdownBtn.getY() && mouseY <= arrowDropdownBtn.getY() + 20) {
            dropdownExpanded = !dropdownExpanded;
            if (cachedProfileBox != null) {
                updateDropdownButtonsLayout(cachedProfileBox.getX() - ARROW_BTN_WIDTH, cachedProfileBox.getY() + 19, dropdownExpanded);
            }
            return true;
        }

        // If dropdown was open and user clicked elsewhere, close dropdown
        if (dropdownExpanded) {
            dropdownExpanded = false;
            updateDropdownButtonsLayout(0, 0, false);
            return true;
        }

        // 3. Check if clicking the EXPORT button
        if (cachedExportButton != null && cachedExportButton.visible
                && mouseX >= cachedExportButton.getX() && mouseX <= cachedExportButton.getX() + cachedExportButton.getWidth()
                && mouseY >= cachedExportButton.getY() && mouseY <= cachedExportButton.getY() + 20) {
            Minecraft.getInstance().setScreenAndShow(new AunExportProfileScreen(screen));
            return true;
        }

        return false;
    }

    public static boolean onClothScreenMouseScrolled(Screen screen, double mouseX, double mouseY, double scrollY) {
        if (!isClothConfigScreen(screen) || !dropdownExpanded) return false;
        List<String> files = listLoadableProfileFiles();
        int maxScroll = Math.max(0, files.size() - MAX_VISIBLE_PROFILES);
        int delta = scrollY > 0 ? -1 : (scrollY < 0 ? 1 : 0);
        dropdownScrollOffset = Math.max(0, Math.min(maxScroll, dropdownScrollOffset + delta));
        if (cachedProfileBox != null) {
            updateDropdownButtonsLayout(cachedProfileBox.getX() - ARROW_BTN_WIDTH, cachedProfileBox.getY() + 19, true);
        }
        return true;
    }

    public static void selectProfileFromDropdown(Screen screen, int visibleSlotIndex) {
        List<String> files = listLoadableProfileFiles();
        int fileIdx = dropdownScrollOffset + visibleSlotIndex;
        if (fileIdx >= 0 && fileIdx < files.size()) {
            String chosen = files.get(fileIdx);
            dropdownExpanded = false;
            updateDropdownButtonsLayout(0, 0, false);
            if (cachedProfileBox != null) {
                cachedProfileBox.setValue(chosen);
            }
            AunConfig cfg = AunConfigManager.getRealConfig();
            if (cfg.profile != null) {
                cfg.profile.customProfilePath = chosen;
                cfg.profile.profileMode = AunConfig.ProfileMode.OWN_PROFILE;
                initialProfileModeOnOpen = AunConfig.ProfileMode.OWN_PROFILE;
            }
            loadProfileFromFile(getLoadableProfilesDir().resolve(chosen), chosen, screen);
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
