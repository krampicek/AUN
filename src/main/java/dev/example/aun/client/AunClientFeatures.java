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
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;

public final class AunClientFeatures {

    private static KeyMapping zoomKey;
    private static KeyMapping faceIntoKey;
    private static KeyMapping faceAwayKey;

    private static volatile int placeFacingMode = 0;

    /** Better F1 state: 0 = normal, 1 = hide HUD only (hand visible), 2 = hide HUD + hand. */
    private static int f1State = 0;
    private static Field hideGuiField = null;
    private static boolean searchedHideGuiField = false;

    public static boolean inLocalPlayerTickEffects = false;
    public static boolean inGuiHudRender = false;

    private static boolean armorVisualHidden = false;
    private static long lastDamageRevealUntilMs = 0L;
    private static int prevHurtTime = 0;

    private static BlockPos lastLightBlockPos = null;
    private static int lastHeldLightLevel = 0;
    private static Method setSectionDirtyMethod = null;
    private static boolean searchedSectionDirtyMethod = false;

    private static Boolean lastNetherCalcState = null;

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

    public static boolean isConfiguredBindingDown(Minecraft client, String spec) {
        if (client == null || client.getWindow() == null || spec == null || spec.isBlank()) {
            return false;
        }
        String s = spec.trim().toUpperCase(Locale.ROOT).replace(" ", "");
        try {
            long windowHandle = client.getWindow().handle();
            if (s.startsWith("MOUSE")) {
                int btn = Integer.parseInt(s.substring(5)) - 1;
                if (btn >= 0 && btn <= 7) {
                    return GLFW.glfwGetMouseButton(windowHandle, btn) == GLFW.GLFW_PRESS;
                }
            }
            int keyCode = switch (s) {
                case "LCTRL", "CTRL", "LEFTCONTROL" -> InputConstants.KEY_LCONTROL;
                case "RCTRL", "RIGHTCONTROL" -> InputConstants.KEY_RCONTROL;
                case "LALT", "ALT", "LEFTALT" -> InputConstants.KEY_LALT;
                case "RALT", "RIGHTALT" -> InputConstants.KEY_RALT;
                case "LSHIFT", "SHIFT" -> InputConstants.KEY_LSHIFT;
                case "RSHIFT" -> InputConstants.KEY_RSHIFT;
                case "TAB" -> InputConstants.KEY_TAB;
                case "CAPSLOCK" -> InputConstants.KEY_CAPSLOCK;
                default -> s.length() == 1 ? (int) s.charAt(0) : -1;
            };
            if (keyCode > 0) {
                return InputConstants.isKeyDown(client.getWindow(), keyCode);
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /** Called by KeyboardHandlerMixin when F1 is pressed in-game. Returns true to cancel vanilla F1. */
    public static boolean handleF1KeyPress(Minecraft client) {
        if (!AunConfigManager.get().betterF1 || client.screen != null) {
            f1State = 0;
            return false;
        }
        f1State = (f1State + 1) % 3;
        setVanillaHideGui(client.options, f1State == 2);
        return true;
    }

    private static void setVanillaHideGui(Options options, boolean value) {
        if (options == null) {
            return;
        }
        if (!searchedHideGuiField) {
            searchedHideGuiField = true;
            try {
                hideGuiField = Options.class.getDeclaredField("hideGui");
                hideGuiField.setAccessible(true);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        if (hideGuiField != null) {
            try {
                hideGuiField.setBoolean(options, value);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private static void onStartTick(Minecraft client) {
        AunConfig cfg = AunConfigManager.get();
        cfg.syncMirrors();
        if (client.options == null) {
            return;
        }

        if (cfg.disableAdvancementKeybind) {
            while (client.options.keyAdvancements.consumeClick()) {
            }
        }

        if (cfg.disableNarrator && client.options.narrator().get() != NarratorStatus.OFF) {
            client.options.narrator().set(NarratorStatus.OFF);
        }

        if (cfg.borderlessFullscreen && Boolean.TRUE.equals(client.options.exclusiveFullscreen().get())) {
            client.options.exclusiveFullscreen().set(false);
        }

        // Sync /nethercalc command visibility in chat whenever the toggle changes
        if (lastNetherCalcState == null || lastNetherCalcState != cfg.netherCalc) {
            lastNetherCalcState = cfg.netherCalc;
            MinecraftServer server = client.getSingleplayerServer();
            if (server != null) {
                for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                    server.getCommands().sendCommands(sp);
                }
            }
        }
    }

    private static void onEndTick(Minecraft client) {
        if (client.options == null) {
            return;
        }
        AunConfig cfg = AunConfigManager.get();
        cfg.syncMirrors();

        if (cfg.facingPlacement && client.screen == null) {
            boolean into = (faceIntoKey != null && faceIntoKey.isDown())
                    || isConfiguredBindingDown(client, cfg.faceIntoKeybind);
            boolean away = (faceAwayKey != null && faceAwayKey.isDown())
                    || isConfiguredBindingDown(client, cfg.faceAwayKeybind);
            if (into) {
                placeFacingMode = 1;
            } else if (away) {
                placeFacingMode = 2;
            } else {
                placeFacingMode = 0;
            }
        } else {
            placeFacingMode = 0;
        }

        if (client.player != null && client.level != null) {
            int hurt = client.player.hurtTime;
            if (hurt > prevHurtTime && hurt > 0) {
                lastDamageRevealUntilMs = System.currentTimeMillis() + 5000L;
            }
            prevHurtTime = hurt;

            int currentLight = cfg.dynamicLights ? getHeldLightLevel(client.player) : 0;
            BlockPos currentPos = client.player.blockPosition();
            if (currentLight != lastHeldLightLevel
                    || (currentLight > 0 && !currentPos.equals(lastLightBlockPos))) {
                if (lastLightBlockPos != null) {
                    markLightAreaDirty(client, lastLightBlockPos);
                }
                markLightAreaDirty(client, currentPos);
                lastLightBlockPos = currentPos.immutable();
                lastHeldLightLevel = currentLight;
            }
        }
    }

    private static void markLightAreaDirty(Minecraft client, BlockPos center) {
        if (client.level == null || client.levelRenderer == null) {
            return;
        }
        client.level.getLightEngine().checkBlock(center);
        if (!searchedSectionDirtyMethod) {
            searchedSectionDirtyMethod = true;
            try {
                setSectionDirtyMethod = client.levelRenderer.getClass()
                        .getDeclaredMethod("setSectionDirty", int.class, int.class, int.class);
                setSectionDirtyMethod.setAccessible(true);
            } catch (ReflectiveOperationException ignored) {
            }
        }
        if (setSectionDirtyMethod != null) {
            int sx = center.getX() >> 4;
            int sy = center.getY() >> 4;
            int sz = center.getZ() >> 4;
            try {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            setSectionDirtyMethod.invoke(client.levelRenderer, sx + dx, sy + dy, sz + dz);
                        }
                    }
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    public static int getHeldLightLevel(Player player) {
        if (player == null) {
            return 0;
        }
        return Math.max(getStackLightLevel(player.getMainHandItem()), getStackLightLevel(player.getOffhandItem()));
    }

    private static int getStackLightLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (path.equals("soul_torch") || path.equals("soul_lantern") || path.equals("soul_campfire")) {
            return 10;
        }
        if (path.equals("redstone_torch")) {
            return 7;
        }
        if (path.equals("torch") || path.equals("end_rod")) {
            return 14;
        }
        if (path.equals("lantern")
                || path.equals("glowstone")
                || path.equals("sea_lantern")
                || path.equals("shroomlight")
                || path.equals("jack_o_lantern")
                || path.equals("lava_bucket")
                || path.equals("campfire")
                || path.equals("beacon")
                || path.contains("froglight")) {
            return 15;
        }
        if (path.equals("glow_berries") || path.equals("glow_ink_sac") || path.equals("blaze_rod")) {
            return 10;
        }
        if (path.equals("magma_block") || path.equals("crying_obsidian") || path.equals("amethyst_cluster")) {
            return 6;
        }
        return 0;
    }

    public static int computeDynamicLightAt(BlockPos pos) {
        if (!AunConfigManager.get().dynamicLights || pos == null) {
            return 0;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null || client.player == null) {
            return 0;
        }
        int best = 0;
        for (Player player : client.level.players()) {
            int held = getHeldLightLevel(player);
            if (held <= 0) {
                continue;
            }
            double dx = (pos.getX() + 0.5D) - player.getX();
            double dy = (pos.getY() + 0.5D) - player.getEyeY();
            double dz = (pos.getZ() + 0.5D) - player.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            int level = (int) Math.round(held - dist * 1.25D);
            if (level > best) {
                best = level;
            }
        }
        return Math.max(0, Math.min(15, best));
    }

    public static boolean isArmorVisualHidden() {
        return armorVisualHidden;
    }

    public static void toggleArmorVisualHidden() {
        armorVisualHidden = !armorVisualHidden;
    }

    public static boolean shouldHideLocalPlayerArmorNow() {
        AunConfig cfg = AunConfigManager.get();
        cfg.syncMirrors();
        if (!cfg.hideArmor || !armorVisualHidden) {
            return false;
        }
        return System.currentTimeMillis() >= lastDamageRevealUntilMs;
    }

    public static boolean isZooming() {
        AunConfig cfg = AunConfigManager.get();
        if (!cfg.zoom) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.screen != null) {
            return false;
        }
        return (zoomKey != null && zoomKey.isDown()) || isConfiguredBindingDown(client, cfg.zoomKeybind);
    }

    public static boolean shouldHideHudOnly() {
        return AunConfigManager.get().betterF1 && f1State == 1;
    }

    public static int getPlaceFacingMode() {
        return placeFacingMode;
    }

    public static boolean shouldCancelOwnEffectParticle() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null) {
            return false;
        }
        AunConfig cfg = AunConfigManager.get();
        cfg.syncMirrors();
        if (client.options.getCameraType().isFirstPerson()) {
            return cfg.hideOwnParticlesFirstPerson;
        } else {
            return cfg.hideOwnParticlesThirdPerson;
        }
    }
}
