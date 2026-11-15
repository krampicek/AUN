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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.storage.LevelSummary;

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
    private static final Map<String, Long> PLAYTIME_TICKS_CACHE = new ConcurrentHashMap<>();
    private static boolean loadedFavorites = false;
    private static final Pattern PLAY_TIME_PATTERN = Pattern.compile("\"(?:minecraft:)?(?:play_time|play_one_minute|total_world_time)\"\\s*:\\s*(\\d+)");

    private static boolean chestSearchActive = false;
    private static boolean searchFocused = false;
    private static String searchQuery = "";
    private static final List<String> RECENT_CHAT_LINES = new ArrayList<>();

    private static Method faceDrawMethod = null;
    private static boolean searchedFaceDrawMethod = false;

    private static final Map<String, String> ORIGIN_RECIPES = Map.ofEntries(
        Map.entry("redstone_torch", "1x Redstone Dust + 1x Stick"),
        Map.entry("torch", "1x Coal/Charcoal + 1x Stick -> 4x Torches"),
        Map.entry("soul_torch", "1x Coal/Charcoal + 1x Stick + 1x Soul Soil/Sand -> 4x Soul Torches"),
        Map.entry("stick", "2x Planks (vertical) -> 4x Sticks"),
        Map.entry("repeater", "3x Stone + 2x Redstone Torch + 1x Redstone Dust"),
        Map.entry("comparator", "3x Stone + 3x Redstone Torch + 1x Nether Quartz"),
        Map.entry("piston", "3x Planks + 4x Cobblestone + 1x Iron Ingot + 1x Redstone Dust"),
        Map.entry("sticky_piston", "1x Piston + 1x Slimeball"),
        Map.entry("observer", "6x Cobblestone + 2x Redstone Dust + 1x Nether Quartz"),
        Map.entry("hopper", "5x Iron Ingot (V-shape) + 1x Chest"),
        Map.entry("dropper", "7x Cobblestone + 1x Redstone Dust"),
        Map.entry("dispenser", "7x Cobblestone + 1x Bow + 1x Redstone Dust"),
        Map.entry("chest", "8x Planks (ring)"),
        Map.entry("trapped_chest", "1x Chest + 1x Tripwire Hook"),
        Map.entry("ender_chest", "8x Obsidian + 1x Eye of Ender"),
        Map.entry("shulker_box", "2x Shulker Shells + 1x Chest"),
        Map.entry("barrel", "6x Planks + 2x Wooden Slabs"),
        Map.entry("lectern", "4x Wooden Slabs + 1x Bookshelf"),
        Map.entry("bookshelf", "6x Planks + 3x Books"),
        Map.entry("chiseled_bookshelf", "6x Planks + 3x Wooden Slabs"),
        Map.entry("book", "3x Paper + 1x Leather"),
        Map.entry("paper", "3x Sugar Cane (horizontal) -> 3x Paper"),
        Map.entry("sugar", "1x Sugar Cane or 1x Honey Bottle"),
        Map.entry("fermented_spider_eye", "1x Spider Eye + 1x Brown Mushroom + 1x Sugar"),
        Map.entry("blaze_powder", "1x Blaze Rod -> 2x Blaze Powder"),
        Map.entry("magma_cream", "1x Blaze Powder + 1x Slimeball"),
        Map.entry("glistering_melon_slice", "1x Melon Slice + 8x Gold Nuggets"),
        Map.entry("golden_carrot", "1x Carrot + 8x Gold Nuggets"),
        Map.entry("golden_apple", "1x Apple + 8x Gold Ingots"),
        Map.entry("eye_of_ender", "1x Ender Pearl + 1x Blaze Powder"),
        Map.entry("anvil", "3x Block of Iron + 4x Iron Ingots"),
        Map.entry("smithing_table", "2x Iron Ingots + 4x Planks"),
        Map.entry("grindstone", "2x Sticks + 1x Stone Slab + 2x Planks"),
        Map.entry("stonecutter", "1x Iron Ingot + 3x Stone"),
        Map.entry("blast_furnace", "5x Iron Ingots + 1x Furnace + 3x Smooth Stone"),
        Map.entry("smoker", "4x Logs + 1x Furnace"),
        Map.entry("furnace", "8x Cobblestone (ring)"),
        Map.entry("crafting_table", "4x Planks (2x2)"),
        Map.entry("enchanting_table", "4x Obsidian + 2x Diamonds + 1x Book"),
        Map.entry("brewing_stand", "1x Blaze Rod + 3x Cobblestone"),
        Map.entry("cauldron", "7x Iron Ingots (U-shape)"),
        Map.entry("beacon", "5x Glass + 3x Obsidian + 1x Nether Star"),
        Map.entry("conduit", "8x Nautilus Shells + 1x Heart of the Sea"),
        Map.entry("shield", "6x Planks + 1x Iron Ingot"),
        Map.entry("bow", "3x Sticks + 3x String"),
        Map.entry("crossbow", "3x Sticks + 2x String + 1x Iron Ingot + 1x Tripwire Hook"),
        Map.entry("arrow", "1x Flint + 1x Stick + 1x Feather -> 4x Arrows"),
        Map.entry("spectral_arrow", "1x Arrow + 4x Glowstone Dust -> 2x Spectral Arrows"),
        Map.entry("mace", "1x Heavy Core + 1x Breeze Rod"),
        Map.entry("wind_charge", "1x Breeze Rod -> 4x Wind Charges"),
        Map.entry("firework_rocket", "1x Paper + 1-3x Gunpowder -> 3x Rockets"),
        Map.entry("tnt", "5x Gunpowder + 4x Sand"),
        Map.entry("rail", "6x Iron Ingots + 1x Stick -> 16x Rails"),
        Map.entry("powered_rail", "6x Gold Ingots + 1x Stick + 1x Redstone Dust -> 6x Powered Rails"),
        Map.entry("detector_rail", "6x Iron Ingots + 1x Stone Pressure Plate + 1x Redstone Dust"),
        Map.entry("activator_rail", "6x Iron Ingots + 2x Sticks + 1x Redstone Torch"),
        Map.entry("minecart", "5x Iron Ingots (U-shape)"),
        Map.entry("bucket", "3x Iron Ingots (V-shape)"),
        Map.entry("shears", "2x Iron Ingots (diagonal)"),
        Map.entry("flint_and_steel", "1x Iron Ingot + 1x Flint"),
        Map.entry("compass", "4x Iron Ingots + 1x Redstone Dust"),
        Map.entry("clock", "4x Gold Ingots + 1x Redstone Dust"),
        Map.entry("spyglass", "1x Amethyst Shard + 2x Copper Ingots"),
        Map.entry("brush", "1x Feather + 1x Copper Ingot + 1x Stick"),
        Map.entry("bundle", "1x String + 1x Leather"),
        Map.entry("lantern", "8x Iron Nuggets + 1x Torch"),
        Map.entry("soul_lantern", "8x Iron Nuggets + 1x Soul Torch"),
        Map.entry("campfire", "3x Sticks + 1x Coal/Charcoal + 3x Logs"),
        Map.entry("soul_campfire", "3x Sticks + 1x Soul Soil/Sand + 3x Logs"),
        Map.entry("lead", "4x String + 1x Slimeball -> 2x Leads"),
        Map.entry("item_frame", "8x Sticks + 1x Leather"),
        Map.entry("glow_item_frame", "1x Item Frame + 1x Glow Ink Sac"),
        Map.entry("painting", "8x Sticks + 1x Wool"),
        Map.entry("armor_stand", "6x Sticks + 1x Smooth Stone Slab"),
        Map.entry("tripwire_hook", "1x Iron Ingot + 1x Stick + 1x Planks -> 2x Hooks")
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

    // -------------------- Horse Info in HorseInventoryScreen --------------------

    public static boolean isHorseInventoryScreen(Screen screen) {
        if (screen == null) return false;
        String cn = screen.getClass().getSimpleName().toLowerCase(Locale.ROOT);
        return cn.contains("horseinventory") || cn.contains("mountinventory");
    }

    public static Component getMountedHorseStatsLabel() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return null;
        Entity vehicle = mc.player.getVehicle();
        if (!(vehicle instanceof LivingEntity mount)) {
            return null;
        }
        double rawSpeed = mount.getAttributeValue(Attributes.MOVEMENT_SPEED);
        double blocksPerSecond = rawSpeed * 42.157787584D;
        double jumpStrength = 0.0D;
        var jumpAttr = mount.getAttribute(Attributes.JUMP_STRENGTH);
        if (jumpAttr != null) {
            double js = jumpAttr.getValue();
            jumpStrength = -0.1817584952D * js * js * js + 3.689713992D * js * js + 2.128599134D * js - 0.343930367D;
            jumpStrength = Math.max(0.0D, jumpStrength);
        }
        return Component.literal(String.format(
                Locale.ROOT,
                "§6Speed: §e%.1f b/s  §bJump: §a%.2f b",
                blocksPerSecond,
                jumpStrength));
    }

    // -------------------- AR & AT: Favourite Worlds & Playtime --------------------

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
        if (summary instanceof LevelSummary ls) {
            return ls.getLevelId();
        }
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
        Object selected = listWidget.getSelected();
        if (selected == null && !listWidget.children().isEmpty()) {
            for (Object child : listWidget.children()) {
                if (child instanceof AbstractSelectionList.Entry<?> e && e.isFocused()) {
                    selected = child;
                    break;
                }
            }
        }
        return getLevelIdFromSummary(extractSummaryFromEntry(selected));
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
        Class<?> c = entry.getClass();
        while (c != null && c != Object.class) {
            for (Field f : c.getDeclaredFields()) {
                if (LevelSummary.class.isAssignableFrom(f.getType()) || f.getType().getSimpleName().equals("LevelSummary")) {
                    try {
                        f.setAccessible(true);
                        return f.get(entry);
                    } catch (ReflectiveOperationException ignored) {}
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    public static String formatWorldPlaytime(String levelId) {
        long ticks = getWorldPlaytimeTicks(levelId);
        long hours = ticks / 72000L;
        if (hours == 0 && ticks > 0) {
            double frac = ticks / 72000.0D;
            return String.format(Locale.ROOT, "%.1fh 🕚", frac);
        }
        return hours + "h 🕚";
    }

    public static long getWorldPlaytimeTicks(String levelId) {
        if (levelId == null || levelId.isEmpty()) return 0L;
        return PLAYTIME_TICKS_CACHE.computeIfAbsent(levelId, AunWorldFavorites::readWorldPlaytimeTicksDisk);
    }

    private static long readWorldPlaytimeTicksDisk(String levelId) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.getLevelSource() == null) return 0L;
            Path worldDir = mc.getLevelSource().getBaseDir().resolve(levelId);
            Path statsDir = worldDir.resolve("stats");
            long totalTicks = 0L;
            if (Files.isDirectory(statsDir)) {
                try (Stream<Path> files = Files.list(statsDir)) {
                    for (Path file : files.toList()) {
                        if (file.toString().endsWith(".json")) {
                            Matcher m = PLAY_TIME_PATTERN.matcher(Files.readString(file));
                            if (m.find()) {
                                totalTicks += Long.parseLong(m.group(1));
                            }
                        }
                    }
                }
            }
            if (totalTicks == 0L) {
                Path levelDat = worldDir.resolve("level.dat");
                if (Files.exists(levelDat)) {
                    CompoundTag root = NbtIo.readCompressed(levelDat, NbtAccounter.unlimitedHeap());
                    CompoundTag data = root.getCompoundOrEmpty("Data");
                    totalTicks = data.getLongOr("Time", 0L);
                }
            }
            return Math.max(0L, totalTicks);
        } catch (Throwable ignored) {
            return 0L;
        }
    }

    // -------------------- B & X: Chat Heads & Chat Text Copy --------------------

    public static void recordChatMessage(String plain) {
        if (plain == null || plain.isBlank()) return;
        RECENT_CHAT_LINES.add(plain);
        if (RECENT_CHAT_LINES.size() > 1000) RECENT_CHAT_LINES.remove(0);
    }

    public static Component decorateChatMessage(Component message) {
        if (message == null) return null;
        String plain = message.getString();
        MutableComponent copy = message.copy();
        if (AunConfigManager.get().chatTextCopy) {
            copy.withStyle(style -> style
                    .withClickEvent(new ClickEvent.CopyToClipboard(plain))
                    .withHoverEvent(new HoverEvent.ShowText(Component.literal("§aClick or Right-Click to copy message"))));
        }
        if (AunConfigManager.get().chatHeads) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getConnection() != null) {
                for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                    String name = info.getProfile().name();
                    if (name != null && !name.isEmpty() && plain.contains(name)) {
                        String[] colors = {"§b", "§a", "§e", "§d", "§6", "§9", "§c"};
                        int idx = Math.abs(name.hashCode()) % colors.length;
                        return Component.literal(colors[idx] + "☻ §r").append(copy);
                    }
                }
            }
        }
        return copy;
    }

    public static boolean copyClickedChatLine(double mouseX, double mouseY, int screenHeight) {
        if (RECENT_CHAT_LINES.isEmpty()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.keyboardHandler == null) return false;
        double scale = mc.options != null ? mc.options.chatScale().get() : 1.0D;
        if (scale <= 0.0D) scale = 1.0D;
        int lineIdx = Math.max(0, (int) (((screenHeight - 40) - mouseY) / (9.0D * scale)));
        int listIdx = RECENT_CHAT_LINES.size() - 1 - Math.min(lineIdx, RECENT_CHAT_LINES.size() - 1);
        String line = RECENT_CHAT_LINES.get(listIdx);
        mc.keyboardHandler.setClipboard(line);
        if (mc.player != null) {
            mc.player.sendOverlayMessage(Component.literal("§aCopied chat message: §f" + line));
        }
        return true;
    }

    // -------------------- AH: Recipe Book Origin Material --------------------

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

    public static int getContainerLeftPos(AbstractContainerScreen<?> screen) {
        try {
            Field f = AbstractContainerScreen.class.getDeclaredField("leftPos");
            f.setAccessible(true);
            return f.getInt(screen);
        } catch (ReflectiveOperationException ignored) {
            return (screen.width - 176) / 2;
        }
    }

    public static int getContainerTopPos(AbstractContainerScreen<?> screen) {
        try {
            Field f = AbstractContainerScreen.class.getDeclaredField("topPos");
            f.setAccessible(true);
            return f.getInt(screen);
        } catch (ReflectiveOperationException ignored) {
            return (screen.height - 166) / 2;
        }
    }

    public static boolean showOriginRecipeFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        String recipe = ORIGIN_RECIPES.get(id);
        if (recipe == null) {
            if (id.endsWith("_planks")) recipe = "1x Matching Log/Stem -> 4x Planks";
            else if (id.endsWith("_wood") || id.endsWith("_hyphae")) recipe = "4x Matching Logs/Stems (2x2) -> 3x Wood";
            else if (id.endsWith("_slab")) recipe = "3x Matching Material (horizontal row) -> 6x Slabs";
            else if (id.endsWith("_stairs")) recipe = "6x Matching Material (stair pattern) -> 4x Stairs";
            else if (id.endsWith("_wall")) recipe = "6x Matching Stone/Brick (2x3) -> 6x Walls";
            else if (id.endsWith("_fence_gate")) recipe = "4x Sticks + 2x Planks -> 1x Fence Gate";
            else if (id.endsWith("_fence")) recipe = "4x Planks + 2x Sticks -> 3x Fences";
            else if (id.endsWith("_door")) recipe = "6x Matching Material (2x3 vertical) -> 3x Doors";
            else if (id.endsWith("_trapdoor")) recipe = "6x Matching Material (3x2) -> 2x Trapdoors";
            else if (id.endsWith("_pressure_plate")) recipe = "2x Matching Material (horizontal) -> 1x Pressure Plate";
            else if (id.endsWith("_button")) recipe = "1x Matching Plank/Stone -> 1x Button";
            else if (id.endsWith("_sign")) recipe = "6x Matching Planks + 1x Stick -> 3x Signs";
            else if (id.endsWith("_hanging_sign")) recipe = "2x Chains + 6x Stripped Logs -> 6x Hanging Signs";
            else if (id.endsWith("_boat")) recipe = "5x Matching Planks (U-shape) -> 1x Boat";
            else if (id.endsWith("_pickaxe")) recipe = "3x Material (top row) + 2x Sticks";
            else if (id.endsWith("_axe")) recipe = "3x Material (corner) + 2x Sticks";
            else if (id.endsWith("_shovel")) recipe = "1x Material + 2x Sticks";
            else if (id.endsWith("_hoe")) recipe = "2x Material (top) + 2x Sticks";
            else if (id.endsWith("_sword")) recipe = "2x Material (vertical) + 1x Stick";
            else if (id.endsWith("_helmet")) recipe = "5x Material (helmet shape)";
            else if (id.endsWith("_chestplate")) recipe = "8x Material (chestplate shape)";
            else if (id.endsWith("_leggings")) recipe = "7x Material (leggings shape)";
            else if (id.endsWith("_boots")) recipe = "4x Material (boots shape)";
            else if (id.endsWith("_ingot")) recipe = "Smelt Raw Ore in Furnace or 9x Nuggets";
            else if (id.endsWith("_nugget")) recipe = "1x Matching Ingot -> 9x Nuggets";
            else if (id.endsWith("_block")) recipe = "9x Matching Ingots/Gems (3x3)";
            else if (id.endsWith("_carpet")) recipe = "2x Matching Wool (horizontal) -> 3x Carpets";
            else if (id.endsWith("_bed")) recipe = "3x Matching Wool + 3x Planks -> 1x Bed";
            else if (id.endsWith("_banner")) recipe = "6x Matching Wool + 1x Stick -> 1x Banner";
            else if (id.endsWith("_glass_pane")) recipe = "6x Matching Glass (3x2) -> 16x Glass Panes";
        }
        if (recipe != null) {
            Component msg = Component.literal("§6[Recipe Origin] §e" + stack.getHoverName().getString() + "§f: " + recipe);
            mc.player.sendOverlayMessage(msg);
            mc.player.sendSystemMessage(msg);
            return true;
