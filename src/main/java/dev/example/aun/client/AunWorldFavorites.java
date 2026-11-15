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

    private static boolean chestSearchActive = false;
    private static boolean searchFocused = false;
    private static String searchQuery = "";
    private static String activeRecipeOriginBanner = null;
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
        Map.entry("barrel", "Sides: 6x [Planks] | Top/Bot center: 2x [Wooden Slab]"),
        Map.entry("lectern", "Top: 3x [Slab] | Mid: [Bookshelf] | Bot: [Slab]"),
        Map.entry("bookshelf", "Top/Bot: 3x [Planks] | Mid: 3x [Book]"),
        Map.entry("book", "3x [Paper] + 1x [Leather]"),
        Map.entry("paper", "3x [Sugar Cane] in a row"),
        Map.entry("blaze_powder", "1x [Blaze Rod] -> 2x Blaze Powder"),
        Map.entry("eye_of_ender", "1x [Ender Pearl] + 1x [Blaze Powder]"),
        Map.entry("anvil", "Top: 3x [Iron Block] | Mid: 1x [Iron Ingot] | Bot: 3x [Iron Ingot]"),
        Map.entry("enchanting_table", "Top: [Book] | Mid: [Diamond][Obsidian][Diamond] | Bot: 3x [Obsidian]"),
        Map.entry("beacon", "Top/Sides: 5x [Glass] | Center: [Nether Star] | Bot: 3x [Obsidian]")
    );

    private AunWorldFavorites() {}

    public static void resetChestSearch() {
        chestSearchActive = false;
        searchFocused = false;
        searchQuery = "";
        activeRecipeOriginBanner = null;
    }

    public static String getActiveRecipeOriginBanner() { return activeRecipeOriginBanner; }
    public static boolean isChestSearchActive() { return chestSearchActive && AunConfigManager.get().chestSearchBar && !searchQuery.isBlank(); }
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

    public static boolean isHorseInventoryScreen(Screen screen) {
        if (screen == null) return false;
        if (screen.getClass().getSimpleName().toLowerCase(Locale.ROOT).contains("horse")) return true;
        Minecraft mc = Minecraft.getInstance();
        return screen instanceof AbstractContainerScreen<?>
                && mc != null && mc.player != null
                && mc.player.getVehicle() instanceof LivingEntity mount
                && mount.getAttribute(Attributes.JUMP_STRENGTH) != null;
    }

    public static Component getMountedHorseInfoLabel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || !(mc.player.getVehicle() instanceof LivingEntity mount)) return Component.empty();
        double blocksPerSec = mount.getAttributeValue(Attributes.MOVEMENT_SPEED) * 42.157787584D;
        double js = mount.getAttribute(Attributes.JUMP_STRENGTH) != null ? mount.getAttributeValue(Attributes.JUMP_STRENGTH) : 0.0D;
        double jumpBlocks = -0.1817584952D * Math.pow(js, 3) + 3.689713992D * Math.pow(js, 2) + 2.128599134D * js - 0.343930367D;
        return Component.literal(String.format(Locale.ROOT, "§eSpeed: §f%.2f b/s  §7|  §bJump: §f%.2f b", blocksPerSec, Math.max(0.0D, jumpBlocks)));
    }

    public static int getContainerLeftPos(AbstractContainerScreen<?> screen) {
        for (Field f : AbstractContainerScreen.class.getDeclaredFields()) {
            if (f.getType() == int.class && (f.getName().equals("leftPos") || f.getName().equals("x"))) {
                try { f.setAccessible(true); return f.getInt(screen); } catch (ReflectiveOperationException ignored) {}
            }
        }
        return (screen.width - 176) / 2;
    }

    public static int getContainerTopPos(AbstractContainerScreen<?> screen) {
        for (Field f : AbstractContainerScreen.class.getDeclaredFields()) {
            if (f.getType() == int.class && (f.getName().equals("topPos") || f.getName().equals("y"))) {
                try { f.setAccessible(true); return f.getInt(screen); } catch (ReflectiveOperationException ignored) {}
            }
        }
        return (screen.height - 166) / 2;
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
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase(Locale.ROOT).replace('_', ' ').contains(query);
    }

    private static synchronized void ensureLoaded() {
        if (loadedFavorites) return;
        loadedFavorites = true;
        if (Files.exists(FAV_FILE)) {
            Properties props = new Properties();
            try (var in = Files.newInputStream(FAV_FILE)) {
                props.load(in);
                for (String key : props.stringPropertyNames()) {
                    try { WORLD_TIERS.put(key, Integer.parseInt(props.getProperty(key, "0"))); } catch (NumberFormatException ignored) {}
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
            props.store(out, "A-U-N Favourite Worlds");
        } catch (IOException ignored) {}
    }

    public static int getWorldTier(String levelId) {
        if (levelId == null || levelId.isEmpty()) return 0;
