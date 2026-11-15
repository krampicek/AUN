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
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AunClientFeatures {

    private static KeyMapping zoomKey;
    private static KeyMapping faceIntoKey;
    private static KeyMapping faceAwayKey;
    private static KeyMapping cachedToggleGuiKey;
    private static boolean searchedToggleGuiKey = false;

    private static volatile int placeFacingMode = 0;
    private static int f1State = 0;
    private static boolean wasF1Down = false;
    private static Boolean lastNetherCalcState = null;

    private static BlockPos lastAccuratePlaceTarget = null;
    private static int useKeyHeldTicks = 0;

    public static Screen currentScreen = null;
    public static boolean inLocalPlayerTickEffects = false;
    public static boolean inGuiHudRender = false;
    public static volatile boolean inGhastFireballHit = false;

    private static boolean armorVisualHidden = false;
    private static final Map<UUID, Long> DAMAGE_REVEAL_UNTIL = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> PREV_HURT_TIME = new ConcurrentHashMap<>();

    private static BlockPos lastLightBlockPos = null;
    private static int lastHeldLightLevel = 0;
    private static Method setSectionDirtyMethod = null;
    private static boolean searchedSectionDirtyMethod = false;

    private AunClientFeatures() {}

    public static void register() {
        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(Aun.MOD_ID, "keys"));
        zoomKey = new KeyMapping("key.aun.zoom", InputConstants.KEY_C, cat);
        faceIntoKey = new KeyMapping("key.aun.face_into", InputConstants.KEY_V, cat);
        faceAwayKey = new KeyMapping("key.aun.face_away", InputConstants.KEY_LCONTROL, cat);
        registerKeyMapping(zoomKey);
        registerKeyMapping(faceIntoKey);
        registerKeyMapping(faceAwayKey);
        ClientTickEvents.START_CLIENT_TICK.register(AunClientFeatures::onStartTick);
        ClientTickEvents.END_CLIENT_TICK.register(AunClientFeatures::onEndTick);
    }

    private static void registerKeyMapping(KeyMapping key) {
        String[][] cands = {
            {"net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper", "registerKeyMapping"},
            {"net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper", "registerKeyBinding"},
            {"net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper", "registerKeyBinding"}
        };
        for (String[] c : cands) {
            try { Class.forName(c[0]).getMethod(c[1], KeyMapping.class).invoke(null, key); return; }
            catch (ReflectiveOperationException ignored) {}
        }
    }

    public static boolean isKeyMappingPhysicallyDown(KeyMapping km) {
        if (km == null) return false;
        if (km.isDown()) return true;
        for (Field f : KeyMapping.class.getDeclaredFields()) {
            if (f.getType() == InputConstants.Key.class) {
                try {
                    f.setAccessible(true);
                    InputConstants.Key k = (InputConstants.Key) f.get(km);
                    if (k != null && k.getType() == InputConstants.Type.KEYSYM && k.getValue() > 0) {
                        return InputConstants.isKeyDown(k.getValue());
                    }
                } catch (ReflectiveOperationException ignored) {}
            }
        }
        return false;
    }

    public static boolean isConfigKeyDown(Minecraft client, String keySpec) {
        if (client == null || keySpec == null || keySpec.isBlank()) return false;
        String clean = keySpec.trim().toUpperCase(Locale.ROOT);
        try {
            if (clean.equals("LCONTROL") || clean.equals("CTRL") || clean.equals("LCTRL")) return InputConstants.isKeyDown(InputConstants.KEY_LCONTROL);
            if (clean.equals("RCONTROL") || clean.equals("RCTRL")) return InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);
            if (clean.equals("LALT") || clean.equals("ALT")) return InputConstants.isKeyDown(InputConstants.KEY_LALT);
            if (clean.equals("LSHIFT") || clean.equals("SHIFT")) return InputConstants.isKeyDown(InputConstants.KEY_LSHIFT);
            int code = InputConstants.class.getField("KEY_" + clean).getInt(null);
            return InputConstants.isKeyDown(code);
        } catch (Throwable ignored) { return false; }
    }

    private static KeyMapping findToggleGuiKey(Options options) {
        if (!searchedToggleGuiKey) {
            searchedToggleGuiKey = true;
            for (Field field : Options.class.getDeclaredFields()) {
                if (KeyMapping.class.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        KeyMapping km = (KeyMapping) field.get(options);
                        if (km != null && km.getName().toLowerCase(Locale.ROOT).contains("togglegui")) {
                            cachedToggleGuiKey = km;
                            break;
                        }
                    } catch (ReflectiveOperationException ignored) {}
                }
            }
        }
        return cachedToggleGuiKey;
    }

    private static void onStartTick(Minecraft client) {
        AunConfig cfg = AunConfigManager.get();
        cfg.syncMirrors();
        if (client.options == null) return;

        if (lastNetherCalcState == null || lastNetherCalcState != cfg.netherCalc) {
            lastNetherCalcState = cfg.netherCalc;
            MinecraftServer srv = client.getSingleplayerServer();
            if (srv != null && srv.getPlayerList() != null) {
                for (ServerPlayer sp : srv.getPlayerList().getPlayers()) srv.getCommands().sendCommands(sp);
            }
        }
        if (cfg.disableAdvancementKeybind) {
            while (client.options.keyAdvancements.consumeClick()) {}
        }
        if (cfg.disableNarrator && client.options.narrator().get() != NarratorStatus.OFF) {
            client.options.narrator().set(NarratorStatus.OFF);
        }
        if (cfg.borderlessFullscreen && Boolean.TRUE.equals(client.options.exclusiveFullscreen().get())) {
            client.options.exclusiveFullscreen().set(false);
        }

        // AC: Better F1 (0 = Normal, 1 = Hide HUD only [Hand stays visible], 2 = Vanilla F1 [Both hidden])
        KeyMapping toggleGuiKey = findToggleGuiKey(client.options);
        if (currentScreen == null) {
            boolean down = toggleGuiKey != null ? isKeyMappingPhysicallyDown(toggleGuiKey) : InputConstants.isKeyDown(InputConstants.KEY_F1);
            if (cfg.betterF1) {
                if (down && !wasF1Down) {
                    if (f1State == 0) {
                        f1State = 1;
                        if (toggleGuiKey != null) {
                            while (toggleGuiKey.consumeClick()) {}
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
        if (client.options == null) return;
        AunConfig cfg = AunConfigManager.get();
        if (client.options.keyUse.isDown()) {
            useKeyHeldTicks++;
        } else {
            useKeyHeldTicks = 0;
            lastAccuratePlaceTarget = null;
        }

        if (cfg.facingPlacement && currentScreen == null) {
            boolean into = isKeyMappingPhysicallyDown(faceIntoKey) || isConfigKeyDown(client, cfg.facingIntoKey);
            boolean away = isKeyMappingPhysicallyDown(faceAwayKey) || isConfigKeyDown(client, cfg.facingAwayKey);
            placeFacingMode = into ? 1 : (away ? 2 : 0);
        } else {
            placeFacingMode = 0;
        }

        if (client.player != null && client.level != null) {
            for (Player p : client.level.players()) {
                int hurt = p.hurtTime;
                int prev = PREV_HURT_TIME.getOrDefault(p.getUUID(), 0);
                if (hurt > prev && hurt > 0) {
                    DAMAGE_REVEAL_UNTIL.put(p.getUUID(), System.currentTimeMillis() + 5000L);
                }
                PREV_HURT_TIME.put(p.getUUID(), hurt);
            }

            int curLight = cfg.dynamicLights ? getHeldLightLevel(client.player) : 0;
            BlockPos curPos = client.player.blockPosition();
            if (curLight != lastHeldLightLevel || (curLight > 0 && !curPos.equals(lastLightBlockPos))) {
                if (lastLightBlockPos != null) markLightAreaDirty(client, lastLightBlockPos);
                markLightAreaDirty(client, curPos);
                lastLightBlockPos = curPos.immutable();
                lastHeldLightLevel = curLight;
            }
        }
    }

    public static boolean shouldAllowAccurateFastPlace(Minecraft client) {
        if (!AunConfigManager.get().accurateBlockPlacement || client.player == null || client.level == null) return false;
        BlockItem held = null;
        if (client.player.getMainHandItem().getItem() instanceof BlockItem b1) held = b1;
        else if (client.player.getOffhandItem().getItem() instanceof BlockItem b2) held = b2;
        if (held == null || held.getBlock() instanceof EntityBlock) return false;
        if (!(client.hitResult instanceof BlockHitResult bhr) || bhr.getType() != HitResult.Type.BLOCK) return false;
        BlockState targetState = client.level.getBlockState(bhr.getBlockPos());
        if (isInteractiveBlock(targetState)) return false;
        BlockPos placePos = bhr.getBlockPos().relative(bhr.getDirection());
        if (placePos.equals(lastAccuratePlaceTarget)) return false;
        lastAccuratePlaceTarget = placePos.immutable();
        return true;
    }

    private static boolean isInteractiveBlock(BlockState state) {
        if (state.getBlock() instanceof EntityBlock) return true;
        String p = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        return p.contains("door") || p.contains("button") || p.contains("lever") || p.contains("gate")
                || p.contains("repeater") || p.contains("comparator") || p.contains("anvil")
                || p.contains("crafting_table") || p.contains("chest") || p.contains("barrel")
                || p.contains("furnace") || p.contains("shulker_box") || p.contains("bed")
                || p.contains("note_block") || p.contains("bell") || p.contains("sign");
    }

    public static boolean isHoldingRightClickContinuously() { return useKeyHeldTicks > 2; }

    public static void refreshPlayerLightArea(Minecraft client) {
        if (client != null && client.player != null) markLightAreaDirty(client, client.player.blockPosition());
    }

    private static void markLightAreaDirty(Minecraft client, BlockPos center) {
        if (client.level == null || client.levelRenderer == null) return;
        client.level.getLightEngine().checkBlock(center);
        if (!searchedSectionDirtyMethod) {
            searchedSectionDirtyMethod = true;
            try {
                setSectionDirtyMethod = client.levelRenderer.getClass().getDeclaredMethod("setSectionDirty", int.class, int.class, int.class);
                setSectionDirtyMethod.setAccessible(true);
            } catch (ReflectiveOperationException ignored) {}
        }
        if (setSectionDirtyMethod != null) {
            int sx = center.getX() >> 4, sy = center.getY() >> 4, sz = center.getZ() >> 4;
            try {
                for (int dx = -1; dx <= 1; dx++)
                    for (int dy = -1; dy <= 1; dy++)
                        for (int dz = -1; dz <= 1; dz++)
                            setSectionDirtyMethod.invoke(client.levelRenderer, sx + dx, sy + dy, sz + dz);
            } catch (ReflectiveOperationException ignored) {}
        }
    }

    public static int getHeldLightLevel(Player p) {
        return p == null ? 0 : Math.max(getStackLightLevel(p.getMainHandItem()), getStackLightLevel(p.getOffhandItem()));
    }

    private static int getStackLightLevel(ItemStack s) {
        if (s == null || s.isEmpty()) return 0;
        String p = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
        if (p.equals("soul_torch") || p.equals("soul_lantern") || p.equals("soul_campfire")) return 10;
        if (p.equals("redstone_torch")) return 7;
        if (p.equals("torch") || p.equals("end_rod")) return 14;
        if (p.equals("lantern") || p.equals("glowstone") || p.equals("sea_lantern") || p.equals("shroomlight")
                || p.equals("jack_o_lantern") || p.equals("lava_bucket") || p.equals("campfire")
                || p.equals("beacon") || p.contains("froglight")) return 15;
        if (p.equals("glow_berries") || p.equals("glow_ink_sac") || p.equals("blaze_rod")) return 10;
        if (p.equals("magma_block") || p.equals("crying_obsidian") || p.equals("amethyst_cluster"))
