package dev.example.aun.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.example.aun.Aun;
import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.NarratorStatus;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Method;

/**
 * Client-side keybinds and state for Zoom (BG), Better F1 (AC),
 * Disable Narrator (J), Disable Advancements Keybind (K), and Facing Placement (A).
 */
public final class AunClientFeatures {

    private static KeyMapping zoomKey;
    private static KeyMapping faceIntoKey;
    private static KeyMapping faceAwayKey;

    /** 0 = normal, 1 = facing into block, 2 = facing away from block. */
    private static volatile int placeFacingMode = 0;

    /** Better F1 state: 0 = normal, 1 = hide HUD only (hand visible), 2 = hide HUD + hand. */
    private static int f1State = 0;
    private static boolean expectedHideGui = false;

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

        // AC: Better F1 (3-state cycle: Normal -> Hide HUD only -> Hide HUD & Hand -> Normal)
        if (cfg.betterF1) {
            if (client.options.hideGui != expectedHideGui) {
                f1State = (f1State + 1) % 3;
                client.options.hideGui = (f1State == 2);
                expectedHideGui = client.options.hideGui;
            }
        } else {
            f1State = client.options.hideGui ? 2 : 0;
            expectedHideGui = client.options.hideGui;
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
