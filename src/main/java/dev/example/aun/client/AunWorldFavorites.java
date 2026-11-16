package dev.example.aun.client;

import dev.example.aun.config.AunConfigManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class AunWorldFavorites {

    private static final Path FAV_FILE = FabricLoader.getInstance().getConfigDir().resolve("aun_favourite_worlds.properties");
    private static final Map<String, Integer> WORLD_TIERS = new ConcurrentHashMap<>();
    private static final Map<String, String> PLAYTIME_TEXT_CACHE = new ConcurrentHashMap<>();
    private static boolean loadedFavorites = false;
    private static final Pattern PLAY_TIME_PATTERN = Pattern.compile("\"minecraft:play_(?:time|one_minute)\"\\s*:\\s*(\\d+)");

    public static EditBox activeChestSearchBox = null;
    private static boolean chestSearchActive = false;
    private static String searchQuery = "";

    // Chat Highlight & Copy State (without touching input box)
    public record RenderedChatLine(int x, int y, String text) {}
    private static final List<RenderedChatLine> VISIBLE_CHAT_LINES = new ArrayList<>();
    private static String selectedChatText = "";
    private static int selLineX = 0, selLineY = 0, selStartIdx = 0, selEndIdx = 0;
    private static boolean draggingChatSelection = false;

    private static final Map<UUID, int[]> HEAD_PIXELS_CACHE = new ConcurrentHashMap<>();
    private static final Set<UUID> HEAD_FETCH_IN_PROGRESS = ConcurrentHashMap.newKeySet();
    private static Method fillMethod = null;
    private static boolean searchedFillMethod = false;

    private AunWorldFavorites() {}

    public static void resetChestSearch() {
        activeChestSearchBox = null;
        chestSearchActive = false;
        searchQuery = "";
        VISIBLE_CHAT_LINES.clear();
        selectedChatText = "";
        selStartIdx = 0;
        selEndIdx = 0;
        draggingChatSelection = false;
    }

    public static boolean isChestSearchActive() {
        return chestSearchActive && AunConfigManager.get().remainingHuds.chestSearchBar && !searchQuery.isBlank();
    }

    public static void setChestSearchActive(boolean active) { chestSearchActive = active; }
    public static void setSearchQuery(String q) { searchQuery = q == null ? "" : q; }

    public static boolean isSearchableContainer(AbstractContainerScreen<?> s) {
        if (isHorseInventoryScreen(s)) return false;
        String cn = s.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        String mn = s.getMenu().getClass().getSimpleName().toLowerCase(Locale.ROOT);
        return cn.contains("container") || cn.contains("chest") || cn.contains("shulker") || cn.contains("hopper")
                || mn.contains("chest") || mn.contains("shulker") || mn.contains("hopper");
    }

    public static boolean isHorseInventoryScreen(Screen s) {
        if (s == null) return false;
        if (s.getClass().getSimpleName().toLowerCase(Locale.ROOT).contains("horse")) return true;
        Minecraft mc = Minecraft.getInstance();
        return s instanceof AbstractContainerScreen<?> && mc != null && mc.player != null
                && mc.player.getVehicle() instanceof LivingEntity m && m.getAttribute(Attributes.JUMP_STRENGTH) != null;
    }

    public static void drawHorseStatsInEmptyBox(GuiGraphicsExtractor g, Font font) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || !(mc.player.getVehicle() instanceof LivingEntity m)) return;
        double bps = m.getAttributeValue(Attributes.MOVEMENT_SPEED) * 42.157787584D;
        double js = m.getAttribute(Attributes.JUMP_STRENGTH) != null ? m.getAttributeValue(Attributes.JUMP_STRENGTH) : 0.0D;
        double jb = Math.max(0.0D, -0.1817584952D * Math.pow(js, 3) + 3.689713992D * Math.pow(js, 2) + 2.128599134D * js - 0.343930367D);
        String line1 = String.format(Locale.ROOT, "Speed: %.2f b/s", bps);
        String line2 = String.format(Locale.ROOT, "Jump: %.2f b", jb);
        try {
            Method drawText = g.getClass().getMethod("text", Font.class, String.class, int.class, int.class, int.class, boolean.class);
            drawText.invoke(g, font, line1, 83, 26, 0xFF404040, false);
            drawText.invoke(g, font, line2, 83, 40, 0xFF404040, false);
        } catch (ReflectiveOperationException ignored) {}
    }

    public static int getContainerLeftPos(AbstractContainerScreen<?> s) {
        for (Field f : AbstractContainerScreen.class.getDeclaredFields()) {
            if (f.getType() == int.class && (f.getName().equals("leftPos") || f.getName().equals("x"))) {
                try { f.setAccessible(true); int v = f.getInt(s); if (v > 0) return v; } catch (ReflectiveOperationException ignored) {}
            }
        }
        return (s.width - 176) / 2;
    }

    public static int getContainerTopPos(AbstractContainerScreen<?> s) {
        for (Field f : AbstractContainerScreen.class.getDeclaredFields()) {
            if (f.getType() == int.class && (f.getName().equals("topPos") || f.getName().equals("y"))) {
                try { f.setAccessible(true); int v = f.getInt(s); if (v > 0) return v; } catch (ReflectiveOperationException ignored) {}
            }
        }
        int rows = (s.getMenu().slots.size() - 36) / 9;
        int imgH = (rows > 0 && rows <= 6) ? (114 + rows * 18) : 166;
        return (s.height - imgH) / 2;
    }

    public static boolean matchesSearch(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String q = searchQuery.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty() || matchesSingleStack(stack, q)) return true;
        ItemContainerContents c = stack.get(DataComponents.CONTAINER);
        if (c != null && c.nonEmptyItemCopyStream().anyMatch(in -> matchesSingleStack(in, q))) return true;
        BundleContents b = stack.get(DataComponents.BUNDLE_CONTENTS);
        return b != null && b.itemCopies().anyMatch(in -> matchesSingleStack(in, q));
    }

    private static boolean matchesSingleStack(ItemStack s, String q) {
        if (s == null || s.isEmpty()) return false;
        String qCompact = q.replace(" ", "").replace("_", "");
        String name = s.getHoverName().getString().toLowerCase(Locale.ROOT);
        String nameCompact = name.replace(" ", "").replace("_", "");
        String id = BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().toLowerCase(Locale.ROOT);
        String idCompact = id.replace("_", "");
        return name.contains(q) || nameCompact.contains(qCompact) || id.contains(q) || idCompact.contains(qCompact);
    }

    public static boolean onScreenMouseClicked(Screen screen, MouseButtonEvent event) {
        EditBox box = activeChestSearchBox;
        if (box != null && screen instanceof AbstractContainerScreen<?> cs) {
            box.setX(getContainerLeftPos(cs) + 80);
            box.setY(getContainerTopPos(cs) + 4);
            if (box.isMouseOver(event.x(), event.y())) {
                if (event.button() == 1) {
                    box.setValue("");
                    setSearchQuery("");
                }
                box.setFocused(true);
                screen.setFocused(box);
                return true;
            } else {
                box.setFocused(false);
            }
        }

        if (screen instanceof AbstractContainerScreen<?> cs
                && AunConfigManager.get().remainingHuds.recipeBookOrigin
                && event.button() == 1) {
            Slot hovered = findHoveredSlot(cs, event.x(), event.y());
            if (hovered != null && hovered.hasItem() && !(hovered.container instanceof Inventory)) {
                if (linkRecipeBookToIngredient(cs, hovered.getItem())) return true;
            }
        }

        if (screen instanceof ChatScreen
                && AunConfigManager.get().remainingHuds.chatTextCopy
                && event.button() == 0
                && event.y() < screen.height - 24) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.font != null) {
                for (RenderedChatLine line : VISIBLE_CHAT_LINES) {
                    if (event.y() >= line.y() - 1 && event.y() <= line.y() + 9) {
                        selectedChatText = line.text();
                        selLineX = line.x();
                        selLineY = line.y();
                        int relX = Math.max(0, (int) event.x() - selLineX);
                        selStartIdx = mc.font.plainSubstrByWidth(selectedChatText, relX).length();
                        selEndIdx = selStartIdx;
                        draggingChatSelection = true;
                        return true;
                    }
                }
            }
            selectedChatText = "";
            selStartIdx = 0;
            selEndIdx = 0;
        }
        return false;
    }

    public static boolean onScreenKeyPressed(Screen screen, KeyEvent event) {
        EditBox box = activeChestSearchBox;
        if (box != null && box.isFocused()) {
            if (event.key() == 256) {
                box.setFocused(false);
                screen.setFocused(null);
                return true;
            }
            if (event.key() == 259) {
                String cur = box.getValue();
                if (!cur.isEmpty()) {
                    box.setValue(cur.substring(0, cur.length() - 1));
                    setSearchQuery(box.getValue());
                }
                return true;
            }
            box.keyPressed(event);
            setSearchQuery(box.getValue());
            return true;
        }

        if (screen instanceof ChatScreen
                && AunConfigManager.get().remainingHuds.chatTextCopy
                && !selectedChatText.isEmpty()
                && selStartIdx != selEndIdx
                && (event.isCopy() || (event.key() == 67 && event.hasControlDown()))) {
            int a = Math.max(0, Math.min(selStartIdx, selEndIdx));
            int b = Math.min(selectedChatText.length(), Math.max(selStartIdx, selEndIdx));
            if (b > a) {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null && mc.keyboardHandler != null) {
                    mc.keyboardHandler.setClipboard(selectedChatText.substring(a, b));
                    if (mc.player != null) mc.player.sendOverlayMessage(Component.literal("§aCopied highlighted chat text!"));
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean onScreenCharTyped(Screen screen, CharacterEvent event) {
        EditBox box = activeChestSearchBox;
        if (box != null && box.isFocused()) {
            String ch = event.codepointAsString();
            if (ch != null && !ch.isEmpty() && ch.charAt(0) >= 32) {
                String cur = box.getValue();
                if (cur.length() < 24) {
                    box.setValue(cur + ch);
                    setSearchQuery(box.getValue());
                }
                return true;
            }
        }
        return false;
    }

    public static void onChatLineRendered(GuiGraphicsExtractor g, Font font, String plain, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || !(AunClientFeatures.currentScreen instanceof ChatScreen chatScreen) || y >= chatScreen.height - 24) return;
        VISIBLE_CHAT_LINES.removeIf(l -> l.y() == y);
        VISIBLE_CHAT_LINES.add(new RenderedChatLine(x, y, plain));
        if (VISIBLE_CHAT_LINES.size() > 60) VISIBLE_CHAT_LINES.remove(0);

        if (draggingChatSelection) {
            if (mc.mouseHandler.isLeftPressed() && mc.getWindow() != null) {
                double mx = mc.mouseHandler.xpos() * chatScreen.width / mc.getWindow().getScreenWidth();
                int relX = Math.max(0, (int) mx - selLineX);
                selEndIdx = font.plainSubstrByWidth(selectedChatText, relX).length();
            } else {
                draggingChatSelection = false;
            }
        }

        if (!selectedChatText.isEmpty() && y == selLineY && selStartIdx != selEndIdx) {
            int a = Math.max(0, Math.min(selStartIdx, selEndIdx));
            int b = Math.min(selectedChatText.length(), Math.max(selStartIdx, selEndIdx));
            if (b > a) {
                int x1 = selLineX + font.width(selectedChatText.substring(0, a));
                int x2 = selLineX + font.width(selectedChatText.substring(0, b));
                invokeFill(g, x1, y - 1, x2, y + 9, 0x883377FF);
            }
        }
    }

    private static synchronized void ensureLoaded() {
        if (loadedFavorites) return;
        loadedFavorites = true;
        if (Files.exists(FAV_FILE)) {
            Properties p = new Properties();
            try (var in = Files.newInputStream(FAV_FILE)) {
                p.load(in);
                for (String k : p.stringPropertyNames()) {
                    try { WORLD_TIERS.put(k, Integer.parseInt(p.getProperty(k, "0"))); } catch (NumberFormatException ignored) {}
                }
            } catch (IOException ignored) {}
        }
    }

    private static synchronized void saveFavorites() {
        Properties p = new Properties();
        for (Map.Entry<String, Integer> e : WORLD_TIERS.entrySet()) if (e.getValue() > 0) p.setProperty(e.getKey(), Integer.toString(e.getValue()));
        try (var out = Files.newOutputStream(FAV_FILE)) { p.store(out, "A-U-N Favourite Worlds"); } catch (IOException ignored) {}
    }

    public static int getWorldTier(String id) {
        if (id == null || id.isEmpty()) return 0;
        ensureLoaded();
        return WORLD_TIERS.getOrDefault(id, 0);
    }

    public static String getLevelIdFromSummary(Object sum) {
        if (sum == null) return "";
        try { Object r = sum.getClass().getMethod("getLevelId").invoke(sum); if (r instanceof String s) return s; } catch (ReflectiveOperationException ignored) {}
        return "";
    }

    public static Component getFavoriteButtonLabel(Screen s) {
        int t = getWorldTier(findSelectedWorldId(s));
        return Component.literal(t == 1 ? "§6★ Gold" : (t == 2 ? "§b❖ Diamond" : "☆ Fav: Off"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void cycleSelectedWorldFavorite(Screen s) {
        String id = findSelectedWorldId(s);
        if (id == null || id.isEmpty()) return;
        int next = (getWorldTier(id) + 1) % 3;
        if (next == 0) WORLD_TIERS.remove(id); else WORLD_TIERS.put(id, next);
        saveFavorites();
        AbstractSelectionList<?> list = findWorldSelectionList(s);
        if (list != null) ((List) list.children()).sort(Comparator.comparingInt(e -> -getWorldTier(getLevelIdFromSummary(extractSummaryFromEntry(e)))));
    }

    private static String findSelectedWorldId(Screen s) {
        AbstractSelectionList<?> list = findWorldSelectionList(s);
        return list == null ? "" : getLevelIdFromSummary(extractSummaryFromEntry(list.getSelected()));
    }

    private static AbstractSelectionList<?> findWorldSelectionList(Screen s) {
        if (s == null) return null;
        for (Field f : s.getClass().getDeclaredFields()) {
            if (AbstractSelectionList.class.isAssignableFrom(f.getType())) {
                try { f.setAccessible(true); return (AbstractSelectionList<?>) f.get(s); } catch (ReflectiveOperationException ignored) {}
            }
        }
        return null;
    }

    private static Object extractSummaryFromEntry(Object e) {
        if (e == null) return null;
        for (Field f : e.getClass().getDeclaredFields()) {
            if (f.getType().getSimpleName().contains("LevelSummary")) {
                try { f.setAccessible(true); return f.get(e); } catch (ReflectiveOperationException ignored) {}
            }
        }
        return null;
    }

    public static String getWorldPlaytimeFormatted(String id) {
        return (id == null || id.isEmpty()) ? "0.0h 🕚" : PLAYTIME_TEXT_CACHE.computeIfAbsent(id, AunWorldFavorites::readWorldPlaytimeTextDisk);
    }

    private static String readWorldPlaytimeTextDisk(String id) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getLevelSource() == null) return "0.0h 🕚";
            Path dir = mc.getLevelSource().getBaseDir().resolve(id).resolve("stats");
            long ticks = 0L;
            if (Files.isDirectory(dir)) {
                try (Stream<Path> files = Files.list(dir)) {
                    for (Path f : files.toList()) {
                        if (f.toString().endsWith(".json")) {
                            Matcher m = PLAY_TIME_PATTERN.matcher(Files.readString(f));
                            if (m.find()) ticks += Long.parseLong(m.group(1));
                        }
                    }
                }
            }
            double h = ticks / 72000.0D;
            return h >= 10.0D ? String.format(Locale.ROOT, "%dh 🕚", Math.round(h)) : String.format(Locale.ROOT, "%.1fh 🕚", h);
        } catch (Throwable ignored) { return "0.0h 🕚"; }
    }

    public static void recordChatMessage(String plain) {}

    public static Component prependChatHeadBadge(Component msg) {
        UUID sender = findSenderUuidForChatLine(msg.getString());
        if (sender != null) {
            ensurePlayerHeadLoaded(sender);
            return Component.literal("   ").append(msg);
        }
        return msg;
    }

    public static UUID findSenderUuidForChatLine(String lineText) {
        if (lineText == null || lineText.isEmpty()) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;
        if (mc.player != null) {
            String myName = mc.player.getName().getString();
            if (!myName.isEmpty() && lineText.contains(myName)) return mc.player.getUUID();
        }
        if (mc.getConnection() != null) {
            for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                String n = info.getProfile().name();
                if (n != null && !n.isEmpty() && lineText.contains(n)) return info.getProfile().id();
            }
        }
        return null;
    }

    public static Slot findHoveredSlot(AbstractContainerScreen<?> s, double mx, double my) {
        int lp = getContainerLeftPos(s), tp = getContainerTopPos(s);
        for (Slot sl : s.getMenu().slots) {
            if (sl.isActive() && mx >= lp + sl.x && mx < lp + sl.x + 16 && my >= tp + sl.y && my < tp + sl.y + 16) return sl;
        }
        return null;
    }

    public static boolean linkRecipeBookToIngredient(AbstractContainerScreen<?> screen, ItemStack ingredient) {
        if (ingredient == null || ingredient.isEmpty()) return false;
        Class<?> cls = screen.getClass();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                if (f.getType().getSimpleName().contains("RecipeBookComponent")) {
                    try {
                        f.setAccessible(true);
                        Object rbc = f.get(screen);
                        if (rbc != null && selectIngredientInRecipeBookComponent(rbc, ingredient)) return true;
                    } catch (ReflectiveOperationException ignored) {}
                }
            }
            cls = cls.getSuperclass();
        }
        return false;
    }

    private static boolean selectIngredientInRecipeBookComponent(Object rbc, ItemStack ingredient) {
        Class<?> cls = rbc.getClass();
        while (cls != null && cls != Object.class) {
            for (Field f : cls.getDeclaredFields()) {
                if (EditBox.class.isAssignableFrom(f.getType())) {
                    try {
                        f.setAccessible(true);
                        EditBox searchBox = (EditBox) f.get(rbc);
                        if (searchBox != null) {
                            searchBox.setValue(ingredient.getHoverName().getString());
                            return true;
                        }
                    } catch (ReflectiveOperationException ignored) {}
                }
            }
            cls = cls.getSuperclass();
        }
        return false;
    }

    private static void ensurePlayerHeadLoaded(UUID uuid) {
        if (uuid == null || HEAD_PIXELS_CACHE.containsKey(uuid) || !HEAD_FETCH_IN_PROGRESS.add(uuid)) return;
        CompletableFuture.runAsync(() -> {
            try {
                BufferedImage img = ImageIO.read(URI.create("https://mc-heads.net/avatar/" + uuid.toString().replace("-", "") + "/8.png").toURL());
                if (img != null && img.getWidth() >= 8 && img.getHeight() >= 8) {
                    int[] pixels = new int[64];
                    img.getRGB(0, 0, 8, 8, pixels, 0, 8);
                    HEAD_PIXELS_CACHE.put(uuid, pixels);
                }
            } catch (Throwable ignored) {
            } finally {
                HEAD_FETCH_IN_PROGRESS.remove(uuid);
            }
        });
    }

    private static void invokeFill(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
        if (!searchedFillMethod) {
            searchedFillMethod = true;
            try { fillMethod = g.getClass().getMethod("fill", int.class, int.class, int.class, int.class, int.class); }
            catch (ReflectiveOperationException ignored) {}
        }
        if (fillMethod != null) {
            try { fillMethod.invoke(g, x1, y1, x2, y2, color); } catch (ReflectiveOperationException ignored) {}
        }
    }

    public static void drawPlayerHead8x8(GuiGraphicsExtractor g, UUID uuid, int x, int y) {
        if (uuid == null) return;
        ensurePlayerHeadLoaded(uuid);
        int[] pixels = HEAD_PIXELS_CACHE.get(uuid);
        if (pixels != null) {
            for (int py = 0; py < 8; py++) {
                for (int px = 0; px < 8; px++) {
                    invokeFill(g, x + px, y + py, x + px + 1, y + py + 1, pixels[py * 8 + px] | 0xFF000000);
                }
            }
        } else {
            invokeFill(g, x, y, x + 8, y + 8, 0xFF000000 | (Math.abs(uuid.hashCode()) & 0xFFFFFF));
        }
    }

    public static void renderPlayerHeadsOnLocatorBar(GuiGraphicsExtractor g) {
        if (!AunConfigManager.get().playerHud.headLocator) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) return;
        int cx = g.guiWidth() / 2;
        int by = g.guiHeight() - 30;
        for (Player o : mc.level.players()) {
            if (o == mc.player) continue;
            float diff = Mth.wrapDegrees((float) (Math.toDegrees(Math.atan2(o.getZ() - mc.player.getZ(), o.getX() - mc.player.getX())) - 90.0D - mc.player.getYRot()));
            if (Math.abs(diff) <= 60.0F) {
                int hx = cx + Math.round((diff / 60.0F) * 85.0F) - 4;
                drawPlayerHead8x8(g, o.getUUID(), hx, by);
            }
        }
    }
}
// END OF FILE
