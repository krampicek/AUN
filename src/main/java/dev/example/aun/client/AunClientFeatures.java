package dev.example.aun.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.example.aun.Aun;
import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.NarratorStatus;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

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
    private static boolean wasF1Down = false;
    private static Field hideGuiField = null;
    private static boolean searchedHideGuiField = false;

    /** Zoom state via OptionInstance value field. */
    private static Field optionValueField = null;
    private static boolean searchedOptionValueField = false;
    private static Integer savedUserFov = null;

    /** /nethercalc live command sync state. */
    private static Boolean lastNetherCalcState = null;

    /** Accurate Block Placement tracking. */
    private static BlockPos lastAccuratePlaceTarget = null;

    /** Prevent Accidental Log Stripping tracking. */
    private static int useKeyHeldTicks = 0;

    /** Active GUI Screen tracked via ScreenMixin. */
    public static Screen currentScreen = null;

    public static boolean inLocalPlayerTickEffects = false;
    public static boolean inGuiHudRender = false;

    private static boolean armorVisualHidden = false;
    private static long lastDamageRevealUntilMs = 0L;
    private static int prevHurtTime = 0;

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

    public static boolean isConfigKeyDown(Minecraft client, String keySpec) {
        if (client == null || keySpec == null || keySpec.isBlank()) {
            return false;
        }
        String clean = keySpec.trim().toUpperCase(Locale.ROOT);
        try {
            if (clean.equals("LCONTROL") || clean.equals("CTRL") || clean.equals("LCTRL")) {
                return InputConstants.isKeyDown(InputConstants.KEY_LCONTROL);
            }
            if (clean.equals("RCONTROL") || clean.equals("RCTRL")) {
                return InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);
            }
            if (clean.equals("LALT") || clean.equals("ALT")) {
                return InputConstants.isKeyDown(InputConstants.KEY_LALT);
            }
            if (clean.equals("LSHIFT") || clean.equals("SHIFT")) {
                return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT);
            }
            Field f = InputConstants.class.getField("KEY_" + clean);
            int code = f.getInt(null);
            return InputConstants.isKeyDown(code);
        } catch (Throwable ignored) {
            return false;
