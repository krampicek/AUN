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

/**
 * Helper for Batch 6 features:
 *   - AQ: Chest Search Bar (including inside Shulker Boxes & Bundles)
 *   - AR: Favourite Worlds (Gold & Diamond tiers)
 *   - AT: World Playtime Display (XXXXh 🕚)
 *   - B:  Chat Heads
 *   - X:  Chat Text Copy
 *   - AH: Recipe Book Origin Material
 *   - AP: Head Locator Bar
 */
public final class AunWorldFavorites {

    private static final Path FAV_FILE = FabricLoader.getInstance().getConfigDir().resolve("aun_favourite_worlds.properties");
    private static final Map<String, Integer> WORLD_TIERS = new ConcurrentHashMap<>();
    private static final Map<String, Long> PLAYTIME_HOURS_CACHE = new ConcurrentHashMap<>();
    private static boolean loadedFavorites = false;

    private static final Pattern PLAY_TIME_PATTERN = Pattern.compile("\"minecraft:play_(?:time|one_minute)\"\\s*:\\s*(\\d+)");

    // AQ: Chest Search state
    private static boolean chestSearchActive = false;
    private static boolean searchFocused = false;
    private static String searchQuery = "";

    // X: Recent chat lines for click-to-copy
    private static final List<String> RECENT_CHAT_LINES = new ArrayList<>();

    // AH: Common origin crafting materials reference
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
        Map.entry("fermented_spider_eye", "1x Spider Eye + 1x Brown Mushroom + 1x Sugar"),
        Map.entry("blaze_powder", "1x Blaze Rod"),
        Map.entry("magma_cream", "1x Blaze Powder + 1x Slimeball"),
        Map.entry("glistering_melon_slice", "1x Melon Slice + 8x Gold Nuggets"),
        Map.entry("golden_carrot", "1x Carrot + 8x Gold Nuggets"),
        Map.entry("eye_of_ender", "1x Ender Pearl + 1x Blaze Powder"),
        Map.entry("anvil", "3x Block of Iron + 4x Iron Ingots"),
        Map.entry("enchanting_table", "4x Obsidian + 2x Diamonds + 1x Book"),
        Map.entry("beacon", "5x Glass + 3x Obsidian + 1x Nether Star")
    );

    private AunWorldFavorites() {}

    // -------------------- AQ: Chest Search Bar --------------------

    public static void resetChestSearch() {
        chestSearchActive = false;
        searchFocused = false;
        searchQuery = "";
    }

    public static boolean isChestSearchActive() {
        return chestSearchActive && AunConfigManager.get().chestSearchBar && !searchQuery.isBlank();
    }

    public static void setChestSearchActive(boolean active) {
        chestSearchActive = active;
    }

    public static boolean isSearchFocused() {
        return searchFocused;
    }

    public static void setSearchFocused(boolean focused) {
        searchFocused = focused;
    }

    public static void clearSearchQuery() {
        searchQuery = "";
    }

    public static void appendSearchChar(String ch) {
        if (searchQuery.length() < 24) {
            searchQuery = searchQuery + ch;
        }
    }

    public static void backspaceSearchChar() {
        if (!searchQuery.isEmpty()) {
            searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
        }
    }

    public static Component getSearchButtonLabel() {
        if (searchFocused) {
            return Component.literal("§e🔍 " + searchQuery + "_");
        }
        if (searchQuery.isEmpty()) {
            return Component.literal("§7🔍 Search...");
        }
        return Component.literal("§a🔍 " + searchQuery);
    }

    public static boolean isSearchableContainer(AbstractContainerScreen<?> screen) {
        String className = screen.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        if (className.contains("container")
                || className.contains("chest")
                || className.contains("shulker")
                || className.contains("hopper")
                || className.contains("dispenser")) {
            return true;
        }
        String menuName = screen.getMenu().getClass().getSimpleName().toLowerCase(Locale.ROOT);
        return menuName.contains("chest")
                || menuName.contains("shulker")
                || menuName.contains("hopper")
                || menuName.contains("dispenser");
    }

    public static boolean matchesSearch(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        String query = searchQuery.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return true;
        }
        if (matchesSingleStack(stack, query)) {
            return true;
        }
        // Check inside Shulker Boxes / Containers
        ItemContainerContents container = stack.get(DataComponents.CONTAINER);
        if (container != null) {
            boolean foundInShulker = container.nonEmptyItemCopyStream().anyMatch(inner -> matchesSingleStack(inner, query));
            if (foundInShulker) {
                return true;
            }
        }
        // Check inside Bundles
        BundleContents bundle = stack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundle != null) {
            return bundle.itemCopies().anyMatch(inner -> matchesSingleStack(inner, query));
        }
        return false;
    }

    private static boolean matchesSingleStack(ItemStack stack, String query) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        String displayName = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        if (displayName.contains(query)) {
            return true;
        }
        String idPath = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase(Locale.ROOT).replace('_', ' ');
        return idPath.contains(query);
    }

    // -------------------- AR & AT: Favourite Worlds & Playtime --------------------

    private static synchronized void ensureLoaded() {
        if (loadedFavorites) {
            return;
        }
        loadedFavorites = true;
        if (Files.exists(FAV_FILE)) {
            Properties props = new Properties();
            try (var in = Files.newInputStream(FAV_FILE)) {
                props.load(in);
                for (String key : props.stringPropertyNames()) {
                    try {
                        WORLD_TIERS.put(key, Integer.parseInt(props.getProperty(key, "0")));
                    } catch (NumberFormatException ignored) {
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }

    private static synchronized void saveFavorites() {
        Properties props = new Properties();
        for (Map.Entry<String, Integer> entry : WORLD_TIERS.entrySet()) {
            if (entry.getValue() > 0) {
                props.setProperty(entry.getKey(), Integer.toString(entry.getValue()));
            }
        }
        try (var out = Files.newOutputStream(FAV_FILE)) {
            props.store(out, "A-U-N Favourite Worlds (1 = Gold, 2 = Diamond)");
        } catch (IOException ignored) {
        }
    }

    public static int getWorldTier(String levelId) {
        if (levelId == null || levelId.isEmpty()) {
            return 0;
        }
        ensureLoaded();
        return WORLD_TIERS.getOrDefault(levelId, 0);
    }

    public static String getLevelIdFromSummary(Object summary) {
        if (summary == null) {
            return "";
        }
        try {
            Method m = summary.getClass().getMethod("getLevelId");
            Object res = m.invoke(summary);
            if (res instanceof String s) {
                return s;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return "";
    }

    public static Component getFavoriteButtonLabel(Screen selectWorldScreen) {
        String levelId = findSelectedWorldId(selectWorldScreen);
        int tier = getWorldTier(levelId);
        if (tier == 1) {
            return Component.literal("§6★ Gold");
        }
        if (tier == 2) {
            return Component.literal("§b❖ Diamond");
        }
        return Component.literal("☆ Classic");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void cycleSelectedWorldFavorite(Screen selectWorldScreen) {
        String levelId = findSelectedWorldId(selectWorldScreen);
        if (levelId == null || levelId.isEmpty()) {
            return;
        }
        int nextTier = (getWorldTier(levelId) + 1) % 3;
        if (nextTier == 0) {
            WORLD_TIERS.remove(levelId);
        } else {
            WORLD
