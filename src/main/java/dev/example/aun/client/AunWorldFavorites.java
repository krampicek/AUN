package dev.example.aun.client;

import dev.example.aun.config.AunConfigManager;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
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
        return Component.literal("☆ Fav: Off");
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
                try { f.setAccessible(true); return (AbstractSelectionList<?>) f.get(screen); } catch (ReflectiveOperationException ignored) {}
            }
        }
        return null;
    }

    private static Object extractSummaryFromEntry(Object entry) {
        if (entry == null) return null;
        for (Field f : entry.getClass().getDeclaredFields()) {
            if (f.getType().getSimpleName().contains("LevelSummary")) {
                try { f.setAccessible(true); return f.get(entry); } catch (ReflectiveOperationException ignored) {}
            }
        }
        return null;
    }

    public static String getWorldPlaytimeFormatted(String levelId) {
        if (levelId == null || levelId.isEmpty()) return "0.0h 🕚";
        return PLAYTIME_TEXT_CACHE.computeIfAbsent(levelId, AunWorldFavorites::readWorldPlaytimeTextDisk);
    }

    private static String readWorldPlaytimeTextDisk(String levelId) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getLevelSource() == null) return "0.0h 🕚";
            Path statsDir = mc.getLevelSource().getBaseDir().resolve(levelId).resolve("stats");
            long totalTicks = 0L;
            if (Files.isDirectory(statsDir)) {
                try (Stream<Path> files = Files.list(statsDir)) {
                    for (Path file : files.toList()) {
                        if (file.toString().endsWith(".json")) {
                            Matcher m = PLAY_TIME_PATTERN.matcher(Files.readString(file));
                            if (m.find()) totalTicks += Long.parseLong(m.group(1));
                        }
                    }
                }
            }
            double hours = totalTicks / 72000.0D;
            return hours >= 10.0D ? String.format(Locale.ROOT, "%dh 🕚", Math.round(hours)) : String.format(Locale.ROOT, "%.1fh 🕚", hours);
        } catch (Throwable ignored) {
            return "0.0h 🕚";
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
                return Component.literal("   ").append(message);
            }
        }
        return message;
    }

    public static PlayerInfo findSenderForChatLine(String lineText) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getConnection() == null || lineText == null || lineText.isEmpty()) return null;
        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            String name = info.getProfile().name();
            if (name != null && !name.isEmpty() && lineText.contains(name)) return info;
        }
        return null;
    }

    public static boolean copyClickedChatLine(double mouseX, double mouseY, int screenHeight) {
        if (RECENT_CHAT_LINES.isEmpty()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.keyboardHandler == null) return false;
        int lineIdx = Math.max(0, (int) (((screenHeight - 40) - mouseY) / 9.0));
        int listIdx = RECENT_CHAT_LINES.size() - 1 - Math.min(lineIdx, RECENT_CHAT_LINES.size() - 1);
        mc.keyboardHandler.setClipboard(RECENT_CHAT_LINES.get(listIdx).trim());
        if (mc.player != null) mc.player.sendOverlayMessage(Component.literal("§aCopied chat message to clipboard!"));
        return true;
    }

    public static boolean handleSprintDropAllInContainer(AbstractContainerScreen<?> screen) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.gameMode == null || mc.getWindow() == null) return false;
        double mx = mc.mouseHandler.xpos() * screen.width / mc.getWindow().getScreenWidth();
        double my = mc.mouseHandler.ypos() * screen.height / mc.getWindow().getScreenHeight();
        Slot hovered = findHoveredSlot(screen, mx, my);
        if (hovered == null || !hovered.hasItem() || !hovered.mayPickup(mc.player)) return false;
        ItemStack template = hovered.getItem().copy();
        int menuId = screen.getMenu().containerId;
        for (Slot slot : screen.getMenu().slots) {
            if (slot.container == hovered.container && slot.hasItem() && slot.mayPickup(mc.player)
                    && ItemStack.isSameItemSameComponents(slot.getItem(), template)) {
                mc.gameMode.handleInventoryMouseClick(menuId, slot.index, 1, ContainerInput.THROW, mc.player);
            }
        }
        return true;
    }

    public static Slot findHoveredSlot(AbstractContainerScreen<?> screen, double mouseX, double mouseY) {
        int leftPos = getContainerLeftPos(screen);
        int topPos = getContainerTopPos(screen);
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
            else if (id.endsWith("_slab")) recipe = "3x Matching Blocks (row) -> 6x Slabs";
            else if (id.endsWith("_stairs")) recipe = "6x Matching Blocks (stairs) -> 4x Stairs";
            else if (id.endsWith("_ingot")) recipe = "Smelt Raw Ore or craft 9x Nuggets";
            else if (id.endsWith("_block")) recipe = "9x Matching Ingot/Gem in 3x3 grid";
            else recipe = "Craft or obtain " + stack.getHoverName().getString();
        }
        activeRecipeOriginBanner = "§6[Recipe: " + stack.getHoverName().getString() + "] §f" + recipe;
        mc.player.sendOverlayMessage(Component.literal(activeRecipeOriginBanner));
        return true;
    }

    public static void renderPlayerHeadsOnLocatorBar(GuiGraphicsExtractor graphics) {
        if (!AunConfigManager.get().headLocator) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null || mc.getConnection() == null) return;
        int centerX = graphics.guiWidth() / 2;
        int barY = graphics.guiHeight() - 29;
        for (Player other : mc.level.players()) {
            if (other == mc.player) continue;
            double dx = other.getX() - mc.player.getX();
            double dz = other.getZ() - mc.player.getZ();
            float diff = Mth.wrapDegrees((float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D - mc.player.getYRot()));
            if (Math.abs(diff) <= 60.0F) {
                int headX = centerX + Math.round((diff / 60.0F) * 85.0F) - 4;
                PlayerInfo info = mc.getConnection().getPlayerInfo(other.getUUID());
                if (info != null) {
                    PlayerFaceRenderer.draw(graphics, info.getSkin(), headX, barY, 8);
                }
            }
        }
    }
}
// END OF FILE
