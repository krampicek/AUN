package dev.example.aun.client;

import dev.example.aun.config.AunConfigManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
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

    private static boolean chestSearchActive = false, searchFocused = false;
    private static String searchQuery = "", activeRecipeOriginBanner = null;
    private static final List<String> RECENT_CHAT_LINES = new ArrayList<>();

    private static final Map<String, String> ORIGIN_RECIPES = Map.ofEntries(
        Map.entry("redstone_torch", "[Redstone Dust] over [Stick] -> 1x Redstone Torch"),
        Map.entry("torch", "[Coal/Charcoal] over [Stick] -> 4x Torch"),
        Map.entry("stick", "[Planks] over [Planks] -> 4x Stick"),
        Map.entry("repeater", "Top: [R.Torch][R.Dust][R.Torch] | Bot: 3x [Stone]"),
        Map.entry("comparator", "Top: [R.Torch] | Mid: [R.Torch][Quartz][R.Torch] | Bot: 3x [Stone]"),
        Map.entry("piston", "Top: 3x [Planks] | Mid: [Cobble][Iron][Cobble] | Bot: [Cobble][Redstone][Cobble]"),
        Map.entry("sticky_piston", "[Slimeball] over [Piston] -> 1x Sticky Piston"),
        Map.entry("observer", "Top/Bot: 3x [Cobble] | Mid: [Redstone][Redstone][Quartz]"),
        Map.entry("hopper", "5x [Iron Ingot] in V-shape around 1x [Chest]"),
        Map.entry("chest", "8x [Planks] in a ring"),
        Map.entry("barrel", "Sides: 6x [Planks] | Top/Bot: 2x [Wooden Slab]"),
        Map.entry("lectern", "Top: 3x [Slab] | Mid: [Bookshelf] | Bot: [Slab]"),
        Map.entry("bookshelf", "Top/Bot: 3x [Planks] | Mid: 3x [Book]"),
        Map.entry("book", "3x [Paper] + 1x [Leather]"),
        Map.entry("paper", "3x [Sugar Cane] in a row"),
        Map.entry("blaze_powder", "1x [Blaze Rod] -> 2x Blaze Powder"),
        Map.entry("eye_of_ender", "1x [Ender Pearl] + 1x [Blaze Powder]"),
        Map.entry("anvil", "Top: 3x [Iron Block] | Mid/Bot: 4x [Iron Ingot]"),
        Map.entry("enchanting_table", "Top: [Book] | Mid: [Diamond][Obsidian][Diamond] | Bot: 3x [Obsidian]"),
        Map.entry("beacon", "Top/Sides: 5x [Glass] | Center: [Nether Star] | Bot: 3x [Obsidian]")
    );

    private AunWorldFavorites() {}

    public static void resetChestSearch() { chestSearchActive = false; searchFocused = false; searchQuery = ""; activeRecipeOriginBanner = null; }
    public static String getActiveRecipeOriginBanner() { return activeRecipeOriginBanner; }
    public static boolean isChestSearchActive() { return chestSearchActive && AunConfigManager.get().chestSearchBar && !searchQuery.isBlank(); }
    public static void setChestSearchActive(boolean active) { chestSearchActive = active; }
    public static boolean isSearchFocused() { return searchFocused; }
    public static void setSearchFocused(boolean focused) { searchFocused = focused; }
    public static void clearSearchQuery() { searchQuery = ""; }
    public static void appendSearchChar(String ch) { if (searchQuery.length() < 24) searchQuery += ch; }
    public static void backspaceSearchChar() { if (!searchQuery.isEmpty()) searchQuery = searchQuery.substring(0, searchQuery.length() - 1); }

    public static Component getSearchButtonLabel() {
        if (searchFocused) return Component.literal("§e🔍 " + searchQuery + "_");
        return Component.literal(searchQuery.isEmpty() ? "§7🔍 Search..." : "§a🔍 " + searchQuery);
    }

    public static boolean isSearchableContainer(AbstractContainerScreen<?> s) {
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

    public static Component getMountedHorseInfoLabel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || !(mc.player.getVehicle() instanceof LivingEntity m)) return Component.empty();
        double bps = m.getAttributeValue(Attributes.MOVEMENT_SPEED) * 42.157787584D;
        double js = m.getAttribute(Attributes.JUMP_STRENGTH) != null ? m.getAttributeValue(Attributes.JUMP_STRENGTH) : 0.0D;
        double jb = -0.1817584952D * Math.pow(js, 3) + 3.689713992D * Math.pow(js, 2) + 2.128599134D * js - 0.343930367D;
        return Component.literal(String.format(Locale.ROOT, "§eSpeed: §f%.2f b/s  §7|  §bJump: §f%.2f b", bps, Math.max(0.0D, jb)));
    }

    public static int getContainerLeftPos(AbstractContainerScreen<?> s) {
        for (Field f : AbstractContainerScreen.class.getDeclaredFields()) {
            if (f.getType() == int.class && (f.getName().equals("leftPos") || f.getName().equals("x"))) {
                try { f.setAccessible(true); return f.getInt(s); } catch (ReflectiveOperationException ignored) {}
            }
        }
        return (s.width - 176) / 2;
    }

    public static int getContainerTopPos(AbstractContainerScreen<?> s) {
        for (Field f : AbstractContainerScreen.class.getDeclaredFields()) {
            if (f.getType() == int.class && (f.getName().equals("topPos") || f.getName().equals("y"))) {
                try { f.setAccessible(true); return f.getInt(s); } catch (ReflectiveOperationException ignored) {}
            }
        }
        return (s.height - 166) / 2;
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
        return s.getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)
                || BuiltInRegistries.ITEM.getKey(s.getItem()).getPath().toLowerCase(Locale.ROOT).replace('_', ' ').contains(q);
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

    public static void recordChatMessage(String plain) {
        if (plain == null || plain.isBlank()) return;
        RECENT_CHAT_LINES.add(plain);
        if (RECENT_CHAT_LINES.size() > 100) RECENT_CHAT_LINES.remove(0);
    }

    public static Component prependChatHeadBadge(Component msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getConnection() == null) return msg;
        String text = msg.getString();
        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            String n = info.getProfile().name();
            if (n != null && !n.isEmpty() && text.contains(n)) {
                String[] c = {"§b", "§a", "§e", "§d", "§6", "§9", "§c"};
                return Component.literal(c[Math.abs(n.hashCode()) % c.length] + "§l[☺] §r").append(msg);
            }
        }
        return msg;
    }

    public static boolean copyClickedChatLine(double mx, double my, int sh) {
        if (RECENT_CHAT_LINES.isEmpty()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.keyboardHandler == null) return false;
        int idx = RECENT_CHAT_LINES.size() - 1 - Math.min(Math.max(0, (int) (((sh - 40) - my) / 9.0)), RECENT_CHAT_LINES.size() - 1);
        mc.keyboardHandler.setClipboard(RECENT_CHAT_LINES.get(idx).trim());
        if (mc.player != null) mc.player.sendOverlayMessage(Component.literal("§aCopied chat message to clipboard!"));
        return true;
    }

    public static boolean handleSprintDropAllInContainer(AbstractContainerScreen<?> s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.getWindow() == null) return false;
        double mx = mc.mouseHandler.xpos() * s.width / mc.getWindow().getScreenWidth();
        double my = mc.mouseHandler.ypos() * s.height / mc.getWindow().getScreenHeight();
        Slot hov = findHoveredSlot(s, mx, my);
        if (hov == null || !hov.hasItem() || !hov.mayPickup(mc.player)) return false;
        ItemStack tpl = hov.getItem().copy();
        Method clickM = null;
        for (Method m : AbstractContainerScreen.class.getDeclaredMethods()) {
            if (m.getParameterCount() == 4 && m.getParameterTypes()[0] == Slot.class) { m.setAccessible(true); clickM = m; break; }
        }
        for (Slot sl : s.getMenu().slots) {
            if (sl.container == hov.container && sl.hasItem() && sl.mayPickup(mc.player) && ItemStack.isSameItemSameComponents(sl.getItem(), tpl)) {
                if (clickM != null) {
                    try { clickM.invoke(s, sl, sl.index, 1, ContainerInput.THROW); } catch (ReflectiveOperationException ignored) {}
                }
            }
        }
        return true;
    }

    public static Slot findHoveredSlot(AbstractContainerScreen<?> s, double mx, double my) {
        int lp = getContainerLeftPos(s), tp = getContainerTopPos(s);
        for (Slot sl : s.getMenu().slots) {
            if (sl.isActive() && mx >= lp + sl.x && mx < lp + sl.x + 16 && my >= tp + sl.y && my < tp + sl.y + 16) return sl;
        }
        return null;
    }

    public static boolean isCraftingIngredientSlot(AbstractContainerScreen<?> s, Slot sl) { return !(sl.container instanceof Inventory); }

    public static boolean showOriginRecipeFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        String r = ORIGIN_RECIPES.get(id);
        if (r == null) {
            if (id.endsWith("_planks")) r = "1x Matching Log/Stem -> 4x Planks";
            else if (id.endsWith("_slab")) r = "3x Matching Blocks (row) -> 6x Slabs";
            else if (id.endsWith("_stairs")) r = "6x Matching Blocks (stairs) -> 4x Stairs";
            else if (id.endsWith("_ingot")) r = "Smelt Raw Ore or craft 9x Nuggets";
            else if (id.endsWith("_block")) r = "9x Matching Ingot/Gem in 3x3 grid";
            else r = "Craft or obtain " + stack.getHoverName().getString();
        }
        activeRecipeOriginBanner = "§6[Recipe: " + stack.getHoverName().getString() + "] §f" + r;
        mc.player.sendOverlayMessage(Component.literal(activeRecipeOriginBanner));
        return true;
    }

    public static void renderPlayerHeadsOnLocatorBar(GuiGraphicsExtractor g) {
        if (!AunConfigManager.get().headLocator) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) return;
        int cx = g.guiWidth() / 2, by = g.guiHeight() - 31;
        for (Player o : mc.level.players()) {
            if (o == mc.player) continue;
            float diff = Mth.wrapDegrees((float) (Math.toDegrees(Math.atan2(o.getZ() - mc.player.getZ(), o.getX() - mc.player.getX())) - 90.0D - mc.player.getYRot()));
            if (Math.abs(diff) <= 60.0F) {
                int hx = cx + Math.round((diff / 60.0F) * 85.0F) - 8;
                String n = o.getName().getString();
                String tag = "§b[☺" + n.substring(0, Math.min(3, n.length())) + "]";
                try {
                    g.getClass().getMethod("text", net.minecraft.client.gui.Font.class, String.class, int.class, int.class, int.class, boolean.class)
                            .invoke(g, mc.font, tag, hx, by, 0xFFFFFFFF, true);
                } catch (ReflectiveOperationException ignored) {}
            }
        }
    }
}
// END OF FILE
