package dev.example.aun.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.example.aun.Aun;
import dev.example.aun.config.AunConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.lang.reflect.Method;

/**
 * Fullbright: press H (changeable in the controls menu, category "A-U-N") to switch full
 * brightness on and off. It works by giving your own client a hidden, endless night vision
 * effect - the server never knows about it.
 */
public final class FullbrightHandler {

    private static KeyMapping toggleKey;
    private static boolean enabled;
    private static boolean ours; // true while the night vision effect on the player is ours

    private FullbrightHandler() {}

    public static void register() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Aun.MOD_ID, "main"));
        toggleKey = new KeyMapping("key.aun.fullbright", InputConstants.KEY_H, category);
        registerKeyMapping(toggleKey);
        ClientTickEvents.END_CLIENT_TICK.register(FullbrightHandler::tick);
    }

    /** Fabric renamed its key helper between versions, so try the known names. */
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
                // try the next one
            }
        }
        Aun.LOGGER.warn("Could not register the fullbright key");
    }

    private static void tick(Minecraft client) {
        if (client.player == null) {
            ours = false;
            return;
        }
        boolean allowed = AunConfigManager.get().fullbright;
        while (toggleKey.consumeClick()) {
            if (allowed) {
                enabled = !enabled;
            }
        }
        if (enabled && allowed) {
            if (!client.player.hasEffect(MobEffects.NIGHT_VISION)) {
                client.player.addEffect(new MobEffectInstance(
                    MobEffects.NIGHT_VISION, MobEffectInstance.INFINITE_DURATION, 0, false, false, false));
                ours = true;
            }
        } else {
            if (ours) {
                client.player.removeEffect(MobEffects.NIGHT_VISION);
                ours = false;
            }
        }
    }
}
