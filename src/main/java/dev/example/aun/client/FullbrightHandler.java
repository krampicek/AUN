package dev.example.aun.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.example.aun.Aun;
import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public final class FullbrightHandler {

    private static KeyMapping keyBinding;
    private static boolean active = false;
    private static boolean wasKeyDown = false;

    private FullbrightHandler() {}

    public static void register() {
        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Aun.MOD_ID, "main"));
        keyBinding = new KeyMapping("key.aun.fullbright", InputConstants.KEY_H, cat);
        ClientTickEvents.END_CLIENT_TICK.register(FullbrightHandler::onEndTick);
    }

    private static void onEndTick(Minecraft client) {
        AunConfig cfg = AunConfigManager.get();
        if ((cfg.general != null && !cfg.general.modEnabled) || !cfg.fullbright) {
            if (active) {
                active = false;
                AunClientFeatures.refreshPlayerLightArea(client);
            }
            wasKeyDown = false;
            return;
        }
        if (AunClientFeatures.currentScreen != null) {
            wasKeyDown = false;
            return;
        }
        boolean down = AunClientFeatures.isKeyMappingPhysicallyDown(keyBinding)
                || AunClientFeatures.isConfigKeyDown(client, cfg.fullbrightKey);
        if (down && !wasKeyDown) {
            active = !active;
            AunClientFeatures.refreshPlayerLightArea(client);
            AunClientFeatures.sendFeatureStatusOverlay(client, "Fullbright", active);
        }
        wasKeyDown = down;
    }

    public static boolean isFullbrightActive() {
        AunConfig cfg = AunConfigManager.get();
        return (cfg.general == null || cfg.general.modEnabled) && cfg.fullbright && active;
    }
}
// END OF FILE
