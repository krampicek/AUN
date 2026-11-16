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
    public static void setSearchQuery(String q) { searchQuery = q == null ? ""
