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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Client-side keybinds and state for Zoom (BG), Better F1 (AC),
 * Disable Narrator (J), Disable Advancements Keybind (K), Facing Placement (A),
 * Borderless Fullscreen (AU), Hide Own Particles (AE), Hide Armor (BB),
 * Dynamic Lights (AD), and Raised Hotbar (AI).
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

    /** True while LocalPlayer is ticking its own status effect particles. */
    public static boolean inLocalPlayerTickEffects = false;

    /** True while Gui.extractRenderState is drawing the HUD. */
    public static boolean inGuiHudRender = false;

    /** BB: Hide armor visual toggle controlled by the eye icon in InventoryScreen. */
    private static boolean armorVisualHidden = false;
    private static long lastDamageRevealUntilMs = 0L;
    private static int prevHurtTime = 0;

    /** AD: Dynamic lights tracking. */
    private static BlockPos lastLightBlockPos = null;
    private static int lastHeldLightLevel = 0;
    private static Method setSectionDirtyMethod = null;
    private static boolean searchedSectionDirtyMethod = false;

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

        // AU: Borderless fullscreen option sync
        if (cfg.borderlessFullscreen && Boolean.TRUE.equals(client.options.exclusiveFullscreen().get())) {
            client.options.exclusiveFullscreen().set(false);
        }

        // AC: Better F1 (3-state cycle: Normal -> Hide HUD only -> Hide HUD & Hand -> Normal)
        KeyMapping toggleGuiKey = findToggleGuiKey(client.options);
        if (toggleGuiKey != null) {
            boolean down = toggleGuiKey.isDown();
            if (cfg.betterF1) {
                if (down && !wasF1Down) {
                    if (f1State == 0) {
                        f1State = 1;
                        while (toggleGuiKey.consumeClick()) {
                            // swallowed so hand stays visible
                        }
                    } else if (f1State == 1) {
                        f1State = 2;
                    } else {
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

        if (client.player != null && client.level != null) {
            // BB: Reveal armor for 5 seconds when taking damage
            int hurt = client.player.hurtTime;
            if (hurt > prevHurtTime && hurt > 0) {
                lastDamageRevealUntilMs = System.currentTimeMillis() + 5000L;
            }
            prevHurtTime = hurt;

            // AD: Dynamic lights chunk refresh when player moves or switches light items
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
        // Soul items give a smaller light level (10)
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
        if (!AunConfigManager.get().hideArmor || !armorVisualHidden) {
            return false;
        }
        return System.currentTimeMillis() >= lastDamageRevealUntilMs;
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

    public static boolean shouldCancelOwnEffectParticle() {
        if (!inLocalPlayerTickEffects) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null) {
            return false;
        }
        AunConfig cfg = AunConfigManager.get();
        if (client.options.getCameraType().isFirstPerson()) {
            return cfg.hideOwnParticlesFirstPerson;
        } else {
            return cfg.hideOwnParticlesThirdPerson;
        }
    }
}
