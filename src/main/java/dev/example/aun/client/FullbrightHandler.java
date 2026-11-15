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

/**
 * Fullbright: applies full brightness inside LightTextureMixin so the Nether sky and fog
 * never turn neon red. Can be toggled in-game with the configured key (default H).
 */
public final class FullbrightHandler {

    private static KeyMapping toggleKey;
    private static boolean enabled = true;
    private static boolean wasCustomKeyDown = false;

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
        Aun.LOGGER.warn("Could not register the fullbright key");
    }

    private static void tick(Minecraft client) {
        if (client.player == null) {
            return;
        }
        boolean allowed = AunConfigManager.get().fullbright;
        while (toggleKey.consumeClick()) {
            if (allowed) {
                enabled = !enabled;
            }
        }
        boolean customDown = client.screen == null
                && AunClientFeatures.isConfiguredBindingDown(client, AunConfigManager.get().fullbrightKeybind);
        if (customDown && !wasCustomKeyDown && allowed && toggleKey.isDefault()) {
            enabled = !enabled;
        }
        wasCustomKeyDown = customDown;

        if (!allowed) {
            enabled = true;
        }
        // Clean up any leftover Night Vision effect from older versions
        if (client.player.hasEffect(MobEffects.NIGHT_VISION)
                && client.player.getEffect(MobEffects.NIGHT_VISION) != null
                && client.player.getEffect(MobEffects.NIGHT_VISION).isInfiniteDuration()) {
            client.player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    public static boolean isFullbrightActive() {
        return AunConfigManager.get().fullbright && enabled;
    }
}
