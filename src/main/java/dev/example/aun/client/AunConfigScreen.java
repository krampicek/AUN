package dev.example.aun.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
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
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class AunConfigScreen extends Screen {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_VISIBLE_PROFILES = 5;
    private static final int WIDE_BOX_WIDTH = 240;

    private static AunConfig.ProfileMode initialProfileModeOnOpen = null;
    private static Button ourBrowseButton = null;
    private static Button ourSaveButton = null;

    // Custom wide box & 5-item scrollable dropdown state
    private static boolean profileRowVisibleThisFrame = false;
    private static int customBoxX = 0;
    private static int customBoxY = 0;
    private static EditBox underlyingProfileBox = null;
    private static Button underlyingExportButton = null;
    private static boolean dropdownExpanded = false;
    private static int dropdownScrollOffset = 0;

    private static Method fillMethod = null;
    private static Method textMethod = null;
    private static boolean searchedDrawMethods = false;

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
        profileRowVisibleThisFrame = false;

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

    public static void registerBottomButtons(Button browseBtn, Button saveBtn) {
        ourBrowseButton = browseBtn;
        ourSaveButton = saveBtn;
        dropdownExpanded = false;
        dropdownScrollOffset = 0;
    }

    public static void tickClothConfigScreen(Screen screen) {
        if (!isClothConfigScreen(screen)) {
            dropdownExpanded = false;
            profileRowVisibleThisFrame = false;
            return;
        }
        layoutBottomBarButtons(screen);
    }

    public static void layoutBottomBarButtons(Screen screen) {
        int centerX = screen.width / 2;
        int bottomY = screen.height - 26;

        if (ourBrowseButton != null) {
            ourBrowseButton.setX(centerX - 204);
            ourBrowseButton.setY(bottomY);
            ourBrowseButton.setWidth(80);
        }
        if (ourSaveButton != null) {
            ourSaveButton.setX(centerX + 100);
            ourSaveButton.setY(bottomY);
            ourSaveButton.setWidth(80);
            ourSaveButton.active = true;
        }

        for (GuiEventListener child : screen.children()) {
            if (child instanceof Button btn && btn != ourBrowseButton && btn != ourSaveButton && btn.getY() >= screen.height - 32) {
                String msg = btn.getMessage().getString().toLowerCase(Locale.ROOT);
                if (msg.contains("cancel") || msg.contains("discard")) {
                    btn.setX(centerX - 118);
                    btn.setY(bottomY);
                    btn.setWidth(100);
                    btn.setMessage(Component.literal("Cancel"));
                } else {
                    btn.setX(centerX - 12);
                    btn.setY(bottomY);
                    btn.setWidth(106);
                    btn.active = true;
                    btn.setMessage(Component.literal("Save & Quit"));
                }
            }
        }
    }

    public static void onAfterScreenRender(Screen screen, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!isClothConfigScreen(screen)) return;
        layoutBottomBarButtons(screen);

        profileRowVisibleThisFrame = false;
        underlyingProfileBox = null;
        underlyingExportButton = null;

        AbstractSelectionList<?> listWidget = findListWidget(screen);
        if (listWidget == null) return;

        Object subCatEntry = findProfileSubCategoryEntry(listWidget);
        if (subCatEntry == null || !isSubCategoryExpanded(subCatEntry)) {
            dropdownExpanded = false;
            return;
        }

        List<EditBox> subBoxes = new ArrayList<>();
        List<Button> subButtons = new ArrayList<>();
        Set<Object> visited = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        collectSubCategoryWidgets(subCatEntry, subBoxes, subButtons, visited, 0);

        if (!subBoxes.isEmpty()) {
            underlyingProfileBox = subBoxes.get(0);
        }

        for (int i = 0; i < subButtons.size(); i++) {
            Button b = subButtons.get(i);
            String txt = b.getMessage().getString();
            if (txt.contains("EXPORT")) {
                underlyingExportButton = b;
                b.setMessage(Component.literal("EXPORT"));
                if (i + 1 < subButtons.size()) {
                    Button resetBtn = subButtons.get(i + 1);
                    if (Math.abs(resetBtn.getY() - b.getY()) <= 2) {
                        resetBtn.visible = false;
                        resetBtn.active = false;
                    }
                }
                break;
            }
        }

        if (underlyingProfileBox != null) {
            int boxY = underlyingProfileBox.getY();
            if (boxY >= 32 && boxY <= screen.height - 42) {
                profileRowVisibleThisFrame = true;
                int rightEdge = underlyingProfileBox.getX() + underlyingProfileBox.getWidth();
                customBoxX = rightEdge - WIDE_BOX_WIDTH;
                customBoxY = boxY;

                Minecraft mc = Minecraft.getInstance();
                Font font = mc.font;
                AunConfig realCfg = AunConfigManager.getRealConfig();
                String currentProfileName = (realCfg.profile != null && realCfg.profile.customProfilePath != null && !realCfg.profile.customProfilePath.isBlank())
                        ? realCfg.profile.customProfilePath
                        : underlyingProfileBox.getValue();
                if (currentProfileName == null || currentProfileName.isBlank()) {
                    currentProfileName = "default_profile.json";
                }

                // Draw wide 240px black box (fits 32+ characters easily) with selectable arrow on the left
                boolean hoveredBox = mouseX >= customBoxX && mouseX < customBoxX + WIDE_BOX_WIDTH
                        && mouseY >= customBoxY && mouseY < customBoxY + 20;
                int borderColor = (hoveredBox || dropdownExpanded) ? 0xFFFFFFFF : 0xFFA0A0A0;

                drawRect(graphics, customBoxX - 1, customBoxY - 1, customBoxX + WIDE_BOX_WIDTH + 1, customBoxY + 20, borderColor);
                drawRect(graphics, customBoxX, customBoxY, customBoxX + WIDE_BOX_WIDTH, customBoxY + 19, 0xFF000000);

                // Selectable arrow compartment on the left side of the box
                drawRect(graphics, customBoxX + 19, customBoxY, customBoxX + 20, customBoxY + 19, borderColor);
                String arrowSymbol = dropdownExpanded ? "▲" : "▼";
                drawString(graphics, font, arrowSymbol, customBoxX + 6, customBoxY + 6, 0xFFFFFFFF);

                // Profile name (up to 35+ characters visible)
                String visibleName = font.plainSubstrByWidth(currentProfileName, WIDE_BOX_WIDTH - 28);
                drawString(graphics, font, visibleName, customBoxX + 25, customBoxY + 6, 0xFFFFFFFF);

                // Render 5-item scrollable dropdown overlay if open
                if (dropdownExpanded) {
                    List<String> files = listLoadableProfileFiles();
                    int maxScroll = Math.max(0, files.size() - MAX_VISIBLE_PROFILES);
                    if (dropdownScrollOffset > maxScroll) dropdownScrollOffset = maxScroll;
                    if (dropdownScrollOffset < 0) dropdownScrollOffset = 0;

                    int visibleCount = Math.min(MAX_VISIBLE_PROFILES, files.size());
                    int dropTop = customBoxY + 21;
                    int dropHeight = visibleCount * 18 + 2;

                    drawRect(graphics, customBoxX - 1, dropTop - 1, customBoxX + WIDE_BOX_WIDTH + 1, dropTop + dropHeight + 1, 0xFFFFFFFF);
                    drawRect(graphics, customBoxX, dropTop, customBoxX + WIDE_BOX_WIDTH, dropTop + dropHeight, 0xFF101010);

                    for (int i = 0; i < visibleCount; i++) {
                        int fileIdx = dropdownScrollOffset + i;
                        String fileName = files.get(fileIdx);
                        int itemY = dropTop + 1 + i * 18;
                        boolean itemHovered = mouseX >= customBoxX && mouseX < customBoxX + WIDE_BOX_WIDTH
                                && mouseY >= itemY && mouseY < itemY + 18;
                        if (itemHovered) {
                            drawRect(graphics, customBoxX + 1, itemY, customBoxX + WIDE_BOX_WIDTH - 6, itemY + 18, 0xFF383838);
                        }
                        String trimmedFile = font.plainSubstrByWidth(fileName, WIDE_BOX_WIDTH - 16);
                        drawString(graphics, font, trimmedFile, customBoxX + 6, itemY + 5, 0xFFFFFFFF);
                    }

                    // Draw scrollbar on the right edge if there are more than 5 files
                    if (files.size() > MAX_VISIBLE_PROFILES) {
                        int trackX = customBoxX + WIDE_BOX_WIDTH - 5;
                        int trackTop = dropTop + 1;
                        int trackHeight = visibleCount * 18;
                        drawRect(graphics, trackX, trackTop, trackX + 4, trackTop + trackHeight, 0xFF2A2A2A);
                        int thumbHeight = Math.max(12, (trackHeight * MAX_VISIBLE_PROFILES) / files.size());
                        int thumbY = trackTop + ((trackHeight - thumbHeight) * dropdownScrollOffset) / maxScroll;
                        drawRect(graphics, trackX, thumbY, trackX + 4, thumbY + thumbHeight, 0xFFD0D0D0);
                    }
                }
            }
        }
    }

    public static boolean onClothScreenMouseClicked(Screen screen, double mouseX, double mouseY, int button) {
        if (!isClothConfigScreen(screen) || button != 0) return false;

        if (dropdownExpanded && profileRowVisibleThisFrame) {
            List<String> files = listLoadableProfileFiles();
            int visibleCount = Math.min(MAX_VISIBLE_PROFILES, files.size());
            int dropTop = customBoxY + 21;
            int dropHeight = visibleCount * 18 + 2;

            if (mouseX >= customBoxX && mouseX <= customBoxX + WIDE_BOX_WIDTH
                    && mouseY >= dropTop && mouseY <= dropTop + dropHeight) {
                int clickedIdx = (int) ((mouseY - (dropTop + 1)) / 18);
                int fileIdx = dropdownScrollOffset + clickedIdx;
                if (fileIdx >= 0 && fileIdx < files.size()) {
                    String chosen = files.get(fileIdx);
                    dropdownExpanded = false;
                    if (underlyingProfileBox != null) {
                        underlyingProfileBox.setValue(chosen);
                    }
                    AunConfig cfg = AunConfigManager.getRealConfig();
                    if (cfg.profile != null) {
                        cfg.profile.customProfilePath = chosen;
                        cfg.profile.profileMode = AunConfig.ProfileMode.OWN_PROFILE;
                        initialProfileModeOnOpen = AunConfig.ProfileMode.OWN_PROFILE;
                    }
                    loadProfileFromFile(getLoadableProfilesDir().resolve(chosen), chosen, screen);
                }
                return true;
            }

            dropdownExpanded = false;
            return true;
        }

        if (profileRowVisibleThisFrame) {
            if (mouseX >= customBoxX && mouseX <= customBoxX + WIDE_BOX_WIDTH
                    && mouseY >= customBoxY && mouseY <= customBoxY + 20) {
                dropdownExpanded = !dropdownExpanded;
                return true;
            }
        }

        if (underlyingExportButton != null && underlyingExportButton.visible
                && mouseX >= underlyingExportButton.getX()
                && mouseX <= underlyingExportButton.getX() + underlyingExportButton.getWidth()
                && mouseY >= underlyingExportButton.getY()
                && mouseY <= underlyingExportButton.getY() + underlyingExportButton.getHeight()) {
            Minecraft.getInstance().setScreenAndShow(new AunExportProfileScreen(screen));
            return true;
        }

        return false;
    }

    public static boolean onClothScreenMouseScrolled(Screen screen, double mouseX, double mouseY, double scrollY) {
        if (!isClothConfigScreen(screen) || !dropdownExpanded || !profileRowVisibleThisFrame) return false;
        List<String> files = listLoadableProfileFiles();
        int maxScroll = Math.max(0, files.size() - MAX_VISIBLE_PROFILES);
        int delta = scrollY > 0 ? -1 : (scrollY < 0 ? 1 : 0);
        dropdownScrollOffset = Math.max(0, Math.min(maxScroll, dropdownScrollOffset + delta));
        return true;
    }

    private static Object findProfileSubCategoryEntry(AbstractSelectionList<?> listWidget) {
        for (Object entry : listWidget.children()) {
            if (entry != null && entry.getClass().getSimpleName().toLowerCase(Locale.ROOT).contains("subcategory")) {
                return entry;
            }
        }
        return null;
    }

    private static boolean isSubCategoryExpanded(Object subCatEntry) {
        try {
            Method m = subCatEntry.getClass().getMethod("isExpanded");
            Object r = m.invoke(subCatEntry);
            if (r instanceof Boolean b) return b;
        } catch (Throwable ignored) {}
        return true;
    }

    private static void collectSubCategoryWidgets(Object obj, List<EditBox> boxes, List<Button> buttons, Set<Object> visited, int depth) {
        if (obj == null || depth > 5 || !visited.add(obj)) return;
        if (obj instanceof EditBox eb) {
            eb.setMaxLength(64);
            boxes.add(eb);
            return;
        }
        if (obj instanceof Button btn) {
            buttons.add(btn);
            return;
        }
        if (obj instanceof Collection<?> col) {
            for (Object item : col) {
                collectSubCategoryWidgets(item, boxes, buttons, visited, depth + 1);
            }
            return;
        }
        Class<?> cls = obj.getClass();
        if (!cls.getName().toLowerCase(Locale.ROOT).contains("clothconfig")) return;
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers())) continue;
                Class<?> ft = f.getType();
                if (EditBox.class.isAssignableFrom(ft)
                        || Button.class.isAssignableFrom(ft)
                        || Collection.class.isAssignableFrom(ft)
                        || ft.getName().toLowerCase(Locale.ROOT).contains("clothconfig")) {
                    try {
                        f.setAccessible(true);
                        collectSubCategoryWidgets(f.get(obj), boxes, buttons, visited, depth + 1);
                    } catch (Throwable ignored) {}
                }
            }
            cls = cls.getSuperclass();
        }
    }

    private static void ensureDrawMethods(GuiGraphicsExtractor g) {
        if (!searchedDrawMethods) {
            searchedDrawMethods = true;
            try { fillMethod = g.getClass().getMethod("fill", int.class, int.class, int.class, int.class, int.class); }
            catch (Throwable ignored) {}
            try { textMethod = g.getClass().getMethod("text", Font.class, String.class, int.class, int.class, int.class, boolean.class); }
            catch (Throwable ignored) {}
        }
    }

    private static void drawRect(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
        ensureDrawMethods(g);
        if (fillMethod != null) {
            try { fillMethod.invoke(g, x1, y1, x2, y2, color); } catch (Throwable ignored) {}
        }
    }

    private static void drawString(GuiGraphicsExtractor g, Font font, String text, int x, int y, int color) {
        ensureDrawMethods(g);
        if (textMethod != null) {
            try { textMethod.invoke(g, font, text, x, y, color, false); } catch (Throwable ignored) {}
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

    public static void exportProfileWithCustomName(Screen clothScreen, String rawName) {
        invokeClothSaveAll(clothScreen);
        AunConfig cfg = AunConfigManager.getRealConfig();
        cfg.syncMirrors();
        try {
            AutoConfig.getConfigHolder(AunConfig.class).save();
        } catch (Throwable ignored) {}

        try {
            Path exportedDir = getExportedProfilesDir();
            String cleanName = (rawName == null || rawName.isBlank()) ? "my_profile" : rawName.trim();
            cleanName = cleanName.replaceAll("[\\\\/:*?\"<>|]", "_");
            if (!cleanName.toLowerCase(Locale.ROOT).endsWith(".json")) {
                cleanName = cleanName + ".json";
            }
            Path exportPath = exportedDir.resolve(cleanName).toAbsolutePath();
            String json = GSON.toJson(cfg);
            Files.writeString(exportPath, json, StandardCharsets.UTF_8);
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
