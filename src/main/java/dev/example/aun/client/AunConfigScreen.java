package dev.example.aun.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
    private static AunConfig.ProfileMode lastSeenProfileMode = null;

    private final Screen parent;

    public AunConfigScreen(Screen parent) {
        super(Component.literal("A-U-N Settings"));
        this.parent = parent;
    }

    @SuppressWarnings("unchecked")
    public static Screen create(Screen parent) {
        lastSeenProfileMode = AunConfigManager.get().general.profileMode;
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

    public static void saveWithoutClosing(Screen clothScreen) {
        if (clothScreen != null) {
            Class<?> cls = clothScreen.getClass();
            while (cls != null && cls != Object.class) {
                for (Method m : cls.getDeclaredMethods()) {
                    if (m.getName().equals("saveAll") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == boolean.class) {
                        try {
                            m.setAccessible(true);
                            m.invoke(clothScreen, false);
                        } catch (Throwable ignored) {}
                        break;
                    }
                }
                cls = cls.getSuperclass();
            }
        }
        AunConfig cfg = AunConfigManager.get();
        if (cfg.general.profileMode != lastSeenProfileMode) {
            lastSeenProfileMode = cfg.general.profileMode;
            applySelectedProfileMode(cfg, clothScreen);
        }
        cfg.syncMirrors();
        try {
            AutoConfig.getConfigHolder(AunConfig.class).save();
        } catch (Throwable ignored) {}
    }

    public static void tickClothConfigScreen(Screen screen) {
        if (!isClothConfigScreen(screen)) {
            wasCustomBoxFocused = false;
            return;
        }
        List<EditBox> boxes = new ArrayList<>();
        collectEditBoxes(screen, boxes);
        // On the General tab, the 2nd EditBox is "Link Your Own Profile"
        if (boxes.size() >= 2) {
            EditBox profileBox = boxes.get(1);
            boolean focused = profileBox.isFocused();
            if (focused && !wasCustomBoxFocused && !fileDialogOpen) {
                profileBox.setFocused(false);
                openProfileFileChooser(screen, profileBox);
            }
            wasCustomBoxFocused = focused;
        } else {
            wasCustomBoxFocused = false;
        }
    }

    private static void collectEditBoxes(GuiEventListener node, List<EditBox> out) {
        if (node instanceof EditBox eb) {
            out.add(eb);
        } else if (node instanceof ContainerEventHandler ceh) {
            for (GuiEventListener child : ceh.children()) {
                collectEditBoxes(child, out);
            }
        }
    }

    public static void openProfileFileChooser(Screen currentScreen, EditBox targetBox) {
        if (fileDialogOpen) return;
        fileDialogOpen = true;
        Path gameDir = FabricLoader.getInstance().getGameDir().toAbsolutePath();

        CompletableFuture.runAsync(() -> {
            try {
                String chosen = null;
                String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
                if (os.contains("win")) {
                    String safeDir = gameDir.toString().replace("'", "''");
                    String ps = "Add-Type -AssemblyName System.Windows.Forms; " +
                            "$f = New-Object System.Windows.Forms.OpenFileDialog; " +
                            "$f.Title = 'Select A-U-N Profile (.json)'; " +
                            "$f.InitialDirectory = '" + safeDir + "'; " +
                            "$f.Filter = 'JSON Profile (*.json)|*.json|All Files (*.*)|*.*'; " +
                            "if ($f.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) { Write-Output$f.FileName }";
                    Process p = new ProcessBuilder("powershell.exe", "-NoProfile", "-Sta", "-Command", ps)
                            .redirectErrorStream(true)
                            .start();
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = r.readLine()) != null) {
                            String trimmed = line.trim();
                            if (!trimmed.isEmpty() && (trimmed.endsWith(".json") || Files.exists(Path.of(trimmed)))) {
                                chosen = trimmed;
                            }
                        }
                    }
                    p.waitFor();
                }

                if (chosen != null && !chosen.isBlank()) {
                    final String finalChosen = chosen;
                    Minecraft.getInstance().execute(() -> {
                        if (targetBox != null) {
                            targetBox.setValue(finalChosen);
                        }
                        AunConfig cfg = AunConfigManager.get();
                        cfg.general.customProfilePath = finalChosen;
                        cfg.general.profileMode = AunConfig.ProfileMode.OWN_PROFILE;
                        lastSeenProfileMode = AunConfig.ProfileMode.OWN_PROFILE;
                        loadProfileFromFile(Path.of(finalChosen), currentScreen);
                    });
                }
            } catch (Throwable ignored) {
            } finally {
                fileDialogOpen = false;
            }
        });
    }

    public static void exportOwnProfile(Screen currentScreen) {
        saveWithoutClosing(currentScreen);
        CompletableFuture.runAsync(() -> {
            try {
                Path gameDir = FabricLoader.getInstance().getGameDir().toAbsolutePath();
                Path exportPath = gameDir.resolve("aun_exported_profile.json").toAbsolutePath();
                Path configPath = FabricLoader.getInstance().getConfigDir().resolve("aun.json");

                if (Files.exists(configPath)) {
                    Files.copy(configPath, exportPath, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.writeString(exportPath, GSON.toJson(AunConfigManager.get()), StandardCharsets.UTF_8);
                }

                String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
                if (os.contains("win")) {
                    new ProcessBuilder("explorer.exe", "/select," + exportPath).start();
                } else if (os.contains("mac")) {
                    new ProcessBuilder("open", "-R", exportPath.toString()).start();
                } else {
                    new ProcessBuilder("xdg-open", gameDir.toString()).start();
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
            AunConfig def = new AunConfig();
            copyConfigValues(def, cfg);
            cfg.general.profileMode = AunConfig.ProfileMode.DEFAULT;
        } else if (cfg.general.profileMode == AunConfig.ProfileMode.OWN_PROFILE) {
            if (cfg.general.customProfilePath != null && !cfg.general.customProfilePath.isBlank()) {
                loadProfileFromFile(Path.of(cfg.general.customProfilePath.trim()), currentScreen);
                return;
            }
        }
        cfg.syncMirrors();
        try {
            AutoConfig.getConfigHolder(AunConfig.class).save();
        } catch (Throwable ignored) {}
        reopenClothScreen(currentScreen);
    }

    public static void loadProfileFromFile(Path file, Screen currentScreen) {
        try {
            if (file != null && Files.exists(file)) {
                String json = Files.readString(file, StandardCharsets.UTF_8);
                AunConfig loaded = GSON.fromJson(json, AunConfig.class);
                if (loaded != null) {
                    AunConfig current = AunConfigManager.get();
                    String keepPath = file.toAbsolutePath().toString();
                    copyConfigValues(loaded, current);
                    current.general.customProfilePath = keepPath;
                    current.general.profileMode = AunConfig.ProfileMode.OWN_PROFILE;
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
            if (java.lang.reflect.Modifier.isStatic(mod) || java.lang.reflect.Modifier.isTransient(mod)) continue;
            try {
                f.setAccessible(true);
                if (f.getType() == boolean.class) {
                    if (obj instanceof AunConfig.GeneralSettingsConfig && f.getName().equals("modEnabled")) {
                        f.setBoolean(obj, true);
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
            if (java.lang.reflect.Modifier.isStatic(mod) || java.lang.reflect.Modifier.isTransient(mod)) continue;
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
