package dev.example.aun.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.example.aun.Aun;
import dev.example.aun.config.AunConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffects;

import java.lang.reflect.Method;

public final class FullbrightHandler {

    private static KeyMapping toggleKey;
    private static boolean enabled = true;
    private static boolean wasCustomKeyDown = false;
    private static boolean lastActiveState = false;

    private FullbrightHandler() {}

    public static void register() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Aun.MOD_ID, "main"));
        toggleKey = new KeyMapping("key.aun.fullbright", InputConstants.KEY_H, category);
        registerKeyMapping(toggleKey);
        ClientTickEvents.END_CLIENT_TICK.register(FullbrightHandler::tick);
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

    private static void tick(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        boolean allowed = AunConfigManager.get().fullbright;

        while (toggleKey.consumeClick()) {
            if (allowed) {
                enabled = !enabled;
            }
        }

        if (AunClientFeatures.currentScreen == null) {
            boolean customDown = AunClientFeatures.isConfigKeyDown(client, AunConfigManager.get().fullbrightKey);
            if (customDown && !wasCustomKeyDown && allowed) {
                enabled = !enabled;
            }
            wasCustomKeyDown = customDown;
        } else {
            wasCustomKeyDown = false;
        }

        if (!allowed) {
            enabled = true;
        }

        var nv = client.player.getEffect(MobEffects.NIGHT_VISION);
        if (nv != null && nv.isInfiniteDuration() && !nv.isVisible()) {
            client.player.removeEffect(MobEffects.NIGHT_VISION);
        }

        boolean activeNow = isFullbrightActive();
        if (activeNow != lastActiveState) {
            lastActiveState = activeNow;
            AunClientFeatures.refreshPlayerLightArea(client);
            if (client.levelRenderer != null) {
                client.levelRenderer.allChanged();
            }
        }
    }

    public static boolean isFullbrightActive() {
        return AunConfigManager.get().fullbright && enabled;
    }
}
