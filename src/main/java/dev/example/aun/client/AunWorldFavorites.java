package dev.example.aun.client;

import dev.example.aun.config.AunConfigManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
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
    private static final Map<String, Long> PLAYTIME_HOURS_CACHE = new ConcurrentHashMap<>();
    private static boolean loadedFavorites = false;
    private static final Pattern PLAY_TIME_PATTERN = Pattern.compile("\"minecraft:play_(?:time|one_minute)\"\\s*:\\s*(\\d+)");

    private static boolean chestSearchActive = false;
    private static boolean searchFocused = false;
    private static String searchQuery = "";
    private static final List<String> RECENT_CHAT_LINES = new ArrayList<>();

    private static final Map<String, String> ORIGIN_RECIPES = Map.ofEntries(
        Map.entry("redstone_torch", "1x Redstone Dust + 1x Stick"),
        Map.entry("torch", "1x Coal/Charcoal + 1x Stick"),
        Map.entry("stick", "2x Planks (vertical)"),
        Map.entry("repeater", "3x Stone + 2x Redstone Torch + 1x Redstone Dust"),
        Map.entry("comparator", "3x Stone + 3x Redstone Torch + 1x Nether Quartz"),
        Map.entry("piston", "3x Planks + 4x Cobblestone + 1x Iron Ingot + 1x Redstone Dust"),
        Map.entry("sticky_piston", "1x Piston + 1x Slimeball"),
        Map.entry("observer", "6x Cobblestone + 2x Redstone Dust + 1x Nether Quartz"),
        Map.entry("hopper", "5x Iron Ingot + 1x Chest"),
        Map.entry("chest", "8x Planks (ring)"),
        Map.entry("barrel", "6x Planks + 2x Wooden Slabs"),
        Map.entry("lectern", "4x Wooden Slabs + 1x Bookshelf"),
        Map.entry("bookshelf", "6x Planks + 3x Books"),
        Map.entry("book", "3x Paper + 1x Leather"),
        Map.entry("paper", "3x Sugar Cane"),
        Map.entry("blaze_powder", "1x Blaze Rod"),
        Map.entry("eye_of_ender", "1x Ender Pearl + 1x Blaze Powder"),
        Map.entry("anvil", "3x Block of Iron + 4x Iron Ingots"),
        Map.entry("enchanting_table", "4x Obsidian + 2x Diamonds + 1x Book"),
        Map.entry("beacon", "5x Glass + 3x Obsidian + 1x Nether Star")
    );

    private AunWorldFavorites() {}

    public static void resetChestSearch() {
        chestSearchActive = false;
        searchFocused = false;
        searchQuery = "";
    }

    public static boolean isChestSearchActive() {
        return chestSearchActive && AunConfigManager.get().chestSearchBar && !searchQuery.isBlank();
    }

    public static void setChestSearchActive(boolean active) { chestSearchActive = active; }
    public static boolean isSearchFocused() { return searchFocused; }
    public static void setSearchFocused(boolean focused) { searchFocused = focused; }
    public static void clearSearchQuery() { searchQuery = ""; }

    public static void appendSearchChar(String ch) {
        if (searchQuery.length() < 24) searchQuery = searchQuery + ch;
    }

    public static void backspaceSearchChar() {
        if (!searchQuery.isEmpty()) searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
    }

    public static Component getSearchButtonLabel() {
        if (searchFocused) return Component.literal("§e🔍 " + searchQuery + "_");
        if (searchQuery.isEmpty()) return Component.literal("§7🔍 Search...");
        return Component.literal("§a🔍 " + searchQuery);
    }

    public static boolean isSearchableContainer(AbstractContainerScreen<?> screen) {
        String cn = screen.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        String mn = screen.getMenu().getClass().getSimpleName().toLowerCase(Locale.ROOT);
        return cn.contains("container") || cn.contains("chest") || cn.contains("shulker") || cn.contains("hopper")
                || mn.contains("chest") || mn.contains("shulker") || mn.contains("hopper");
    }

    public static boolean matchesSearch(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String q = searchQuery.trim().toLowerCase(Locale.ROOT);
        if (q.isEmpty() || matchesSingleStack(stack, q)) return true;
        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        if (container != null && container.nonEmptyItemCopyStream().anyMatch(in -> matchesSingleStack(in, q))) return true;
        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        return bundle != null && bundle.itemCopies().anyMatch(in -> matchesSingleStack(in, q));
    }

    private static boolean matchesSingleStack(ItemStack stack, String query) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) return true;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase(Locale.ROOT).replace('_', ' ');
        return id.contains(query);
    }

    private static synchronized void ensureLoaded() {
        if (loadedFavorites) return;
        loadedFavorites = true;
        if (Files.exists(FAV_FILE)) {
            Properties props = new Properties();
            try (var in = Files.newInputStream(FAV_FILE)) {
                props.load(in);
                for (String key : props.stringPropertyNames()) {
                    try { WORLD_TIERS.put(key, Integer.parseInt(props.getProperty(key, "0"))); }
                    catch (NumberFormatException ignored) {}
                }
            } catch (IOException ignored) {}
        }
    }

    private static synchronized void saveFavorites() {
        Properties props = new Properties();
        for (Map.Entry<String, Integer> e : WORLD_TIERS.entrySet()) {
            if (e.getValue() > 0) props.setProperty(e.getKey(), Integer.toString(e.getValue()));
        }
        try (var out = Files.newOutputStream(FAV_FILE)) {
            props.store(out, "A-U-N Favourite Worlds (1 = Gold, 2 = Diamond)");
        } catch (IOException ignored) {}
    }

    public static int getWorldTier(String levelId) {
        if (levelId == null || levelId.isEmpty()) return 0;
        ensureLoaded();
        return WORLD_TIERS.getOrDefault(levelId, 0);
    }

    public static String getLevelIdFromSummary(Object summary) {
        if (summary == null) return "";
        try {
            Method m = summary.getClass().getMethod("getLevelId");
            Object res = m.invoke(summary);
            if (res instanceof String s) return s;
        } catch (ReflectiveOperationException ignored) {}
        return "";
    }

    public static Component getFavoriteButtonLabel(Screen selectWorldScreen) {
        int tier = getWorldTier(findSelectedWorldId(selectWorldScreen));
        if (tier == 1) return Component.literal("§6★ Gold");
        if (tier == 2) return Component.literal("§b❖ Diamond");
        return Component.literal("☆ Classic");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void cycleSelectedWorldFavorite(Screen selectWorldScreen) {
        String levelId = findSelectedWorldId(selectWorldScreen);
        if (levelId == null || levelId.isEmpty()) return;
        int nextTier = (getWorldTier(levelId) + 1) % 3;
        if (nextTier == 0) WORLD_TIERS.remove(levelId);
        else WORLD_TIERS.put(levelId, nextTier);
        saveFavorites();
        AbstractSelectionList<?> listWidget = findWorldSelectionList(selectWorldScreen);
        if (listWidget != null) {
            List children = listWidget.children();
            children.sort(Comparator.comparingInt(entry -> -getWorldTier(getLevelIdFromSummary(extractSummaryFromEntry(entry)))));
        }
    }

    private static String findSelectedWorldId(Screen selectWorldScreen) {
        AbstractSelectionList<?> listWidget = findWorldSelectionList(selectWorldScreen);
        if (listWidget == null) return "";
        return getLevelIdFromSummary(extractSummaryFromEntry(listWidget.getSelected()));
    }

    private static AbstractSelectionList<?> findWorldSelectionList(Screen screen) {
        if (screen == null) return null;
        for (Field f : screen.getClass().getDeclaredFields()) {
            if (AbstractSelectionList.class.isAssignableFrom(f.getType())) {
                try {
                    f.setAccessible(true);
                    return (AbstractSelectionList<?>) f.get(screen);
                } catch (ReflectiveOperationException ignored) {}
            }
        }
        return null;
    }

    private static Object extractSummaryFromEntry(Object entry) {
        if (entry == null) return null;
        for (Field f : entry.getClass().getDeclaredFields()) {
            if (f.getType().getSimpleName().equals("LevelSummary")) {
                try {
                    f.setAccessible(true);
                    return f.get(entry);
                } catch (ReflectiveOperationException ignored) {}
            }
        }
        return null;
    }

    public static long getWorldPlaytimeHours(String levelId) {
        if (levelId == null || levelId.isEmpty()) return 0L;
        return PLAYTIME_HOURS_CACHE.computeIfAbsent(levelId, AunWorldFavorites::readWorldPlaytimeHoursDisk);
    }

    private static long readWorldPlaytimeHoursDisk(String levelId) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getLevelSource() == null) return 0L;
            Path statsDir = mc.getLevelSource().getBaseDir().resolve(levelId).resolve("stats");
            if (!Files.isDirectory(statsDir)) return 0L;
            long totalTicks = 0L;
            try (Stream<Path> files = Files.list(statsDir)) {
                for (Path file : files.toList()) {
                    if (file.toString().endsWith(".json")) {
                        Matcher m = PLAY_TIME_PATTERN.matcher(Files.readString(file));
                        if (m.find()) totalTicks += Long.parseLong(m.group(1));
                    }
                }
            }
            return totalTicks / 72000L;
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    public static void recordChatMessage(String plain) {
        if (plain == null || plain.isBlank()) return;
        RECENT_CHAT_LINES.add(plain);
        if (RECENT_CHAT_LINES.size() > 100) RECENT_CHAT_LINES.remove(0);
    }

    public static Component prependChatHeadBadge(Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getConnection() == null) return message;
        String text = message.getString();
        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            String name = info.getProfile().name();
            if (name != null && !name.isEmpty() && text.contains(name)) {
                String[] colors = {"§b", "§a", "§e", "§d", "§6", "§9", "§c"};
                int idx = Math.abs(name.hashCode()) % colors.length;
                return Component.literal(colors[idx] + "⬛ §r").append(message);
            }
        }
        return message;
    }

    public static boolean copyClickedChatLine(double mouseX, double mouseY, int screenHeight) {
        if (RECENT_CHAT_LINES.isEmpty()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.keyboardHandler == null) return false;
        int lineIdx = Math.max(0, (int) (((screenHeight - 40) - mouseY) / 9.0));
        int listIdx = RECENT_CHAT_LINES.size() - 1 - Math.min(lineIdx, RECENT_CHAT_LINES.size() - 1);
        String line = RECENT_CHAT_LINES.get(listIdx);
        mc.keyboardHandler.setClipboard(line);
        if (mc.player != null) mc.player.sendOverlayMessage(Component.literal("§aCopied chat message: §f" + line));
        return true;
    }

    public static Slot findHoveredSlot(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        int leftPos = (screen.width - 176) / 2;
        int topPos = (screen.height - 166) / 2;
        for (Slot slot : screen.getMenu().slots) {
            if (!slot.isActive()) continue;
            int sx = leftPos + slot.x;
            int sy = topPos + slot.y;
            if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) return slot;
        }
        return null;
    }

    public static boolean isCraftingIngredientSlot(AbstractContainerScreen<?> screen, Slot slot) {
        return !(slot.container instanceof Inventory);
    }

    public static boolean showOriginRecipeFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        String recipe = ORIGIN_RECIPES.get(id);
        if (recipe == null) {
            if (id.endsWith("_planks")) recipe = "1x Matching Log/Stem -> 4x Planks";
            else if (id.endsWith("_slab")) recipe = "3x Matching Blocks -> 6x Slabs";
            else if (id.endsWith("_stairs")) recipe = "6x Matching Blocks -> 4x Stairs";
            else if (id.endsWith("_ingot")) recipe = "Smelt Raw Ore or 9x Nuggets";
        }
        if (recipe != null) {
            mc.player.sendOverlayMessage(Component.literal("§6[Recipe Origin] §e" + stack.getHoverName().getString() + "§f: " + recipe));
            return true;
        }
        return false;
    }

    public static void updateHeadLocatorOverlay() {
        if (!AunConfigManager.get().headLocator) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) return;
        for (Player other : mc.level.players()) {
            if (other == mc.player) continue;
            double dx = other.getX() - mc.player.getX();
            double dz = other.getZ() - mc.player.getZ();
            float diff = Mth.wrapDegrees((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D - mc.player.getYRot()));
            if (Math.abs(diff) <= 15.0F && mc.player.tickCount % 20 == 0) {
                int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz));
                mc.player.sendOverlayMessage(Component.literal("§b⬛ " + other.getName().getString() + " §7(" + dist + "m)"));
                break;
            }
        }
    }
}
