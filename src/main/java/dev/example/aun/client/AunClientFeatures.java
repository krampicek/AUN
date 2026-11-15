package dev.example.aun.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.example.aun.Aun;
import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.NarratorStatus;
import net.minecraft.client.Options;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Client-side keybinds and state for Zoom (BG), Better F1 (AC),
 * Disable Narrator (J), Disable Advancements Keybind (K), and Facing Placement (A).
 */
public final class AunClientFeatures {

    private static KeyMapping zoomKey;
    private static KeyMapping faceIntoKey;
    private static KeyMapping faceAwayKey;
    private static KeyMapping cachedToggleGuiKey;
    private static boolean searchedToggleGuiKey = false;

    /** 0 = normal, 1 = facing into block, 2 = facing away from block. */
    private static volatile int placeFacingMode = 0;

    /** Better F1 state: 0 = normal, 1 = hide HUD only (hand visible), 2 = hide HUD + hand. */
    private static int f1State = 0;
    private static boolean wasF1Down = false;

    private AunClientFeatures() {}

    public static void register() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(Aun.MOD_ID, "keys"));

        zoomKey = new KeyMapping("key.aun.zoom", InputConstants.KEY_C, category);
        faceIntoKey = new KeyMapping("key.aun.face_into", InputConstants.KEY_V, category);
        faceAwayKey = new KeyMapping("key.aun.face_away", InputConstants.KEY_LCONTROL, category);

        registerKeyMapping(zoomKey);
        registerKeyMapping(faceIntoKey);
        registerKeyMapping(faceAwayKey);

        ClientTickEvents.START_CLIENT_TICK.register(AunClientFeatures::onStartTick);
        ClientTickEvents.END_CLIENT_TICK.register(AunClientFeatures::onEndTick);
    }

    private static void registerKeyMapping(KeyMapping key) {
        String[][] candidates = {
            {"net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper", "registerKeyMapping"},
            {"net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper", "registerKeyBinding"},
            {"net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper", "registerKeyBinding"}
        };
        for (String[] candidate : candidates) {
            try {
                Method method = Class.forName(candidate[0]).getMethod(candidate[1], KeyMapping.class);
                method.invoke(null, key);
                return;
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private static KeyMapping findToggleGuiKey(Options options) {
        if (!searchedToggleGuiKey) {
            searchedToggleGuiKey = true;
            for (Field field : Options.class.getDeclaredFields()) {
                if (KeyMapping.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        KeyMapping km = (KeyMapping) field.get(options);
                        if (km != null && "key.toggleGui".equals(km.getName())) {
                            cachedToggleGuiKey = km;
                            break;
                        }
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
            }
        }
        return cachedToggleGuiKey;
    }

    private static void onStartTick(Minecraft client) {
        if (client.options == null) {
            return;
        }
        AunConfig cfg = AunConfigManager.get();

        // K: Swallow Advancements keybind presses before Minecraft processes them
        if (cfg.disableAdvancementKeybind) {
            while (client.options.keyAdvancements.consumeClick()) {
                // ignored
            }
        }

        // J: Keep Vanilla Narrator disabled
        if (cfg.disableNarrator && client.options.narrator().get() != NarratorStatus.OFF) {
            client.options.narrator().set(NarratorStatus.OFF);
        }

        // AC: Better F1 (3-state cycle: Normal -> Hide HUD only -> Hide HUD & Hand -> Normal)
        KeyMapping toggleGuiKey = findToggleGuiKey(client.options);
        if (toggleGuiKey != null) {
            boolean down = toggleGuiKey.isDown();
            if (cfg.betterF1) {
                if (down && !wasF1Down) {
                    if (f1State == 0) {
                        f1State = 1;
                        // Swallow the first F1 press so vanilla keeps the hand visible while we hide the HUD
                        while (toggleGuiKey.consumeClick()) {
                            // swallowed
                        }
                    } else if (f1State == 1) {
                        // Let vanilla process this F1 press so it hides both HUD and hand
                        f1State = 2;
                    } else {
                        // Let vanilla process this F1 press so it restores both HUD and hand
                        f1State = 0;
                    }
                }
            } else {
                f1State = 0;
            }
            wasF1Down = down;
        }
    }

    private static void onEndTick(Minecraft client) {
        if (client.options == null) {
            return;
        }
        AunConfig cfg = AunConfigManager.get();

        // A: Update directional placement mode
        if (cfg.facingPlacement) {
            if (faceIntoKey != null && faceIntoKey.isDown()) {
                placeFacingMode = 1;
            } else if (faceAwayKey != null && faceAwayKey.isDown()) {
                placeFacingMode = 2;
            } else {
                placeFacingMode = 0;
            }
        } else {
            placeFacingMode = 0;
        }
    }

    public static boolean isZooming() {
        return AunConfigManager.get().zoom && zoomKey != null && zoomKey.isDown();
    }

    public static boolean shouldHideHudOnly() {
        return AunConfigManager.get().betterF1 && f1State == 1;
    }

    public static int getPlaceFacingMode() {
        return placeFacingMode;
    }
}
