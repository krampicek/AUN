package dev.example.aun.client;

import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class AunConfigScreen extends Screen {

    private final Screen parent;
    private int currentTab = 0;
    private int scrollOffset = 0;
    private final Set<String> expandedGroups = new HashSet<>();
    private final List<RowEntry> visibleRows = new ArrayList<>();

    public AunConfigScreen(Screen parent) {
        super(Component.literal("A-U-N Settings"));
        this.parent = parent;
    }

    public static Screen create(Screen parent) {
        return new AunConfigScreen(parent);
    }

    @Override
    protected void init() {
        this.clearWidgets();
        this.visibleRows.clear();
        AunConfig cfg = AunConfigManager.get();

        // Top Category Tabs
        String[] tabs = {"QOL", "Vanilla+", "Performance", "Fixes"};
        int tabWidth = 76;
        int startX = (this.width - (tabs.length * (tabWidth + 4))) / 2;
        for (int i = 0; i < tabs.length; i++) {
            final int tabIdx = i;
            String label = (this.currentTab == i ? "§e§l" : "§7") + tabs[i];
            this.addRenderableWidget(Button.builder(Component.literal(label), btn -> {
                this.currentTab = tabIdx;
                this.scrollOffset = 0;
                this.init();
            }).bounds(startX + i * (tabWidth + 4), 8, tabWidth, 20).build());
        }

        buildRowsForCurrentTab(cfg);

        int listTop = 34;
        int listBottom = this.height - 32;
        int maxVisible = Math.max(1, (listBottom - listTop) / 22);
        if (this.scrollOffset > Math.max(0, this.visibleRows.size() - maxVisible)) {
            this.scrollOffset = Math.max(0, this.visibleRows.size() - maxVisible);
        }

        int rowLeft = this.width / 2 - 155;
        int rowWidth = 310;

        for (int i = 0; i < maxVisible && (i + this.scrollOffset) < this.visibleRows.size(); i++) {
            RowEntry row = this.visibleRows.get(i + this.scrollOffset);
            int y = listTop + i * 22;
            row.buildWidgets(this, rowLeft, y, rowWidth);
        }

        // Scroll Up / Down buttons if list is taller than screen
        if (this.visibleRows.size() > maxVisible) {
            this.addRenderableWidget(Button.builder(Component.literal("▲"), b -> {
                if (this.scrollOffset > 0) { this.scrollOffset--; this.init(); }
            }).bounds(rowLeft + rowWidth + 6, listTop, 20, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("▼"), b -> {
                if (this.scrollOffset < this.visibleRows.size() - maxVisible) { this.scrollOffset++; this.init(); }
            }).bounds(rowLeft + rowWidth + 6, listBottom - 20, 20, 20).build());
        }

        // Bottom Save & Done Button
        this.addRenderableWidget(Button.builder(Component.literal("§aSave & Done"), btn -> {
            saveAndClose();
        }).bounds(this.width / 2 - 60, this.height - 26, 120, 20).build());
    }

    private void buildRowsForCurrentTab(AunConfig cfg) {
        if (this.currentTab == 0) {
            addGroup("Clear Vision", () -> {
                addToggle("  Clear Water", () -> cfg.clearVision.clearWater, v -> cfg.clearVision.clearWater = v);
                addToggle("  Clear Lava", () -> cfg.clearVision.clearLava, v -> cfg.clearVision.clearLava = v);
                addToggle("  Clear Powdered Snow", () -> cfg.clearVision.clearPowderSnow, v -> cfg.clearVision.clearPowderSnow = v);
            });
            addGroup("World Setting", () -> {
                addToggle("  Favourite Worlds", () -> cfg.worldSettings.favouriteWorlds, v -> cfg.worldSettings.favouriteWorlds = v);
                addToggle("  World Playtime Display", () -> cfg.worldSettings.worldPlaytimeDisplay, v -> cfg.worldSettings.worldPlaytimeDisplay = v);
                addToggle("  Deleted World Recycle Bin", () -> cfg.worldSettings.deletedWorldProtection, v -> cfg.worldSettings.deletedWorldProtection = v);
            });
            addGroup("Entities", () -> {
                addToggle("  Reach Behind Item Frames", () -> cfg.entities.reachBehind, v -> cfg.entities.reachBehind = v);
                addToggle("  Reach Behind Signs", () -> cfg.entities.reachBehindSign, v -> cfg.entities.reachBehindSign = v);
                addToggle("  Armor Stand Gear Swap", () -> cfg.entities.armorStandSwap, v -> cfg.entities.armorStandSwap = v);
                addToggle("  Horse Info (In Inventory)", () -> cfg.entities.horseInfo, v -> cfg.entities.horseInfo = v);
                addToggle("  Click To Dismount", () -> cfg.entities.dismountOnClick, v -> cfg.entities.dismountOnClick = v);
            });
            addGroup("Auto Setting", () -> {
                addToggle("  Auto Pickup", () -> cfg.autoSettings.autoPickup, v -> cfg.autoSettings.autoPickup = v);
                addToggle("  Auto-Climb Ladders", () -> cfg.autoSettings.autoClimbLadders, v -> cfg.autoSettings.autoClimbLadders = v);
                addToggle("  Auto Totem", () -> cfg.autoSettings.autoTotem, v -> cfg.autoSettings.autoTotem = v);
            });
            addGroup("HUD", () -> {
                addToggle("  Stack Refill", () -> cfg.hud.stackRefill, v -> cfg.hud.stackRefill = v);
                addToggle("  Better F1", () -> cfg.hud.betterF1, v -> cfg.hud.betterF1 = v);
                addToggle("  Disable Vanilla Narrator", () -> cfg.hud.disableNarrator, v -> cfg.hud.disableNarrator = v);
                addToggle("  Disable Advancements Key", () -> cfg.hud.disableAdvancementKeybind, v -> cfg.hud.disableAdvancementKeybind = v);
                addToggle("  Sprint + Q Drop All Same", () -> cfg.hud.ctrlDropAllSameItems, v -> cfg.hud.ctrlDropAllSameItems = v);
                addToggle("  Expanded Bundle Preview", () -> cfg.hud.bundleFix, v -> cfg.hud.bundleFix = v);
                addToggle("  More Chat History (1000)", () -> cfg.hud.moreChatHistory, v -> cfg.hud.moreChatHistory = v);
                addToggle("  Hide Armor (Eye Toggle)", () -> cfg.hud.hideArmor, v -> cfg.hud.hideArmor = v);
                addToggle("  Central Crosshair", () -> cfg.hud.centralCrosshair, v -> cfg.hud.centralCrosshair = v);
                addToggle("  Raised Hotbar (Bedrock)", () -> cfg.hud.raisedHotbar, v -> cfg.hud.raisedHotbar = v);
                addToggle("  Chat Text Highlight/Copy", () -> cfg.hud.chatTextCopy, v -> cfg.hud.chatTextCopy = v);
                addToggle("  Head Locator Bar", () -> cfg.hud.headLocator, v -> cfg.hud.headLocator = v);
                addToggle("  Recipe Book Origin Link", () -> cfg.hud.recipeBookOrigin, v -> cfg.hud.recipeBookOrigin = v);
                addToggle("  Chat Heads", () -> cfg.hud.chatHeads, v -> cfg.hud.chatHeads = v);
                addToggle("  Hide Own Particles (1st)", () -> cfg.hud.hideOwnParticlesFirstPerson, v -> cfg.hud.hideOwnParticlesFirstPerson = v);
                addToggle("  Hide Own Particles (3rd)", () -> cfg.hud.hideOwnParticlesThirdPerson, v -> cfg.hud.hideOwnParticlesThirdPerson = v);
            });
            addGroup("Hide Nametags", () -> {
                addToggle("  Hide Player Nametags", () -> cfg.hideNametags.hidePlayerNametags, v -> cfg.hideNametags.hidePlayerNametags = v);
                addToggle("  Hide All Mob Nametags", () -> cfg.hideNametags.hideMobNametags, v -> cfg.hideNametags.hideMobNametags = v);
                addToggle("  Hide Hostile Mobs", () -> cfg.hideNametags.hideHostileMobNametags, v -> cfg.hideNametags.hideHostileMobNametags = v);
                addToggle("  Hide Passive Mobs", () -> cfg.hideNametags.hidePassiveMobNametags, v -> cfg.hideNametags.hidePassiveMobNametags = v);
                addToggle("  Hide Tamed Mobs", () -> cfg.hideNametags.hideTamedMobNametags, v -> cfg.hideNametags.hideTamedMobNametags = v);
                addToggleWithText("  Hidden Mob IDs", () -> cfg.hideNametags.hideMobNametags, v -> cfg.hideNametags.hideMobNametags = v,
                        () -> cfg.hideNametags.hiddenMobsList, s -> cfg.hideNametags.hiddenMobsList = s);
            });

            addToggleWithText("Fullbright + Keybind", () -> cfg.fullbright, v -> cfg.fullbright = v, () -> cfg.fullbrightKey, s -> cfg.fullbrightKey = s);
            addToggleWithText("Zoom + Keybind", () -> cfg.zoom, v -> cfg.zoom = v, () -> cfg.zoomKey, s -> cfg.zoomKey = s);
            addToggleWithTwoTexts("Directional Placement", () -> cfg.facingPlacement, v -> cfg.facingPlacement = v,
                    () -> cfg.facingIntoKey, s -> cfg.facingIntoKey = s, () -> cfg.facingAwayKey, s -> cfg.facingAwayKey = s);

            addToggle("Cut Through Tall Grass", () -> cfg.cutThrough, v -> cfg.cutThrough = v);
            addToggle("Double Doors", () -> cfg.doubleDoors, v -> cfg.doubleDoors = v);
            addToggle("Simple Harvest", () -> cfg.simpleHarvest, v -> cfg.simpleHarvest = v);
            addToggle("Fast Leaf Decay", () -> cfg.fastLeafDecay, v -> cfg.fastLeafDecay = v);
            addToggle("It Was Close", () -> cfg.closeCall, v -> cfg.closeCall = v);
            addToggle("Switch Tools While Mining", () -> cfg.autoToolSwitch, v -> cfg.autoToolSwitch = v);
            addToggle("Book Scroll", () -> cfg.bookScroll, v -> cfg.bookScroll = v);
            addToggle("/nethercalc Command", () -> cfg.netherCalc, v -> cfg.netherCalc = v);
            addToggle("Prevent Accidental Log Strip", () -> cfg.noAccidentalLogStripping, v -> cfg.noAccidentalLogStripping = v);
            addToggle("Accurate Block Placement", () -> cfg.accurateBlockPlacement, v -> cfg.accurateBlockPlacement = v);
            addToggle("Borderless Fullscreen", () -> cfg.borderlessFullscreen, v -> cfg.borderlessFullscreen = v);
            addToggle("No Resource Pack Warning", () -> cfg.noResourcePackWarning, v -> cfg.noResourcePackWarning = v);
            addToggle("Enchantment Descriptions", () -> cfg.enchantmentDescriptions, v -> cfg.enchantmentDescriptions = v);
            addToggle("Dynamic Held Lights", () -> cfg.dynamicLights, v -> cfg.dynamicLights = v);
            addToggle("Fadeless Loading Screen", () -> cfg.fadelessLoading, v -> cfg.fadelessLoading = v);
            addToggle("Fast World Quit", () -> cfg.fastQuit, v -> cfg.fastQuit = v);
            addToggle("Fast Language Reload", () -> cfg.fastLanguageReload, v -> cfg.fastLanguageReload = v);
            addToggle("Search Bar In Chests", () -> cfg.chestSearchBar, v -> cfg.chestSearchBar = v);
        } else if (this.currentTab == 1) {
            addGroup("Griefing Protection", () -> {
                addToggle("  Anti-Enderman Pickup", () -> cfg.mobGriefing.antiEndermanPickup, v -> cfg.mobGriefing.antiEndermanPickup = v);
                addToggle("  Anti-Creeper Grief", () -> cfg.mobGriefing.antiCreeperGrief, v -> cfg.mobGriefing.antiCreeperGrief = v);
                addToggle("  Anti-Ghast Fireball Grief", () -> cfg.mobGriefing.antiGhastGrief, v -> cfg.mobGriefing.antiGhastGrief = v);
                addToggle("  Anti-Blaze Fire Grief", () -> cfg.mobGriefing.antiBlazeGrief, v -> cfg.mobGriefing.antiBlazeGrief = v);
                addToggle("  Anti-Ravager Block Break", () -> cfg.mobGriefing.antiRavagerGrief, v -> cfg.mobGriefing.antiRavagerGrief = v);
                addToggle("  Safe Lightning (No Fire)", () -> cfg.mobGriefing.safeLightning, v -> cfg.mobGriefing.safeLightning = v);
                addToggle("  Zombies Don't Break Doors", () -> cfg.mobGriefing.noZombieDoorBreak, v -> cfg.mobGriefing.noZombieDoorBreak = v);
                addToggle("  No Trample (Feather Fall)", () -> cfg.mobGriefing.noCropTrampleFeatherFalling, v -> cfg.mobGriefing.noCropTrampleFeatherFalling = v);
                addToggle("  No Trample (Mobs)", () -> cfg.mobGriefing.noCropTrampleMobs, v -> cfg.mobGriefing.noCropTrampleMobs = v);
            });
            addGroup("Insta-Mine", () -> {
                addToggle("  Insta-Mine Glass", () -> cfg.instaMine.instaMineGlass, v -> cfg.instaMine.instaMineGlass = v);
                addToggle("  Insta-Mine Deepslate (Haste II)", () -> cfg.instaMine.instaMineDeepslateWithHaste, v -> cfg.instaMine.instaMineDeepslateWithHaste = v);
            });
            addToggleWithTwoTexts("Happy Ghast Speed (H% / Up%)", () -> cfg.speedHappyGhast, v -> cfg.speedHappyGhast = v,
                    () -> cfg.happyGhastHorizontalBoost, s -> cfg.happyGhastHorizontalBoost = s,
                    () -> cfg.happyGhastVerticalBoost, s -> cfg.happyGhastVerticalBoost = s);
            addToggle("Tinted Glass Hides Beacon", () -> cfg.hideBeaconBeamBehindTintedGlass, v -> cfg.hideBeaconBeamBehindTintedGlass = v);
            addToggle("No Bats", () -> cfg.noBatSpawning, v -> cfg.noBatSpawning = v);
            addToggle("No \"Too Expensive!\" Anvil", () -> cfg.anvilNoTooExpensive, v -> cfg.anvilNoTooExpensive = v);
            addToggle("Player Drops Never Despawn", () -> cfg.playerDropsNeverDespawn, v -> cfg.playerDropsNeverDespawn = v);
            addToggle("Infinite Villager Trading", () -> cfg.infiniteVillagerTrading, v -> cfg.infiniteVillagerTrading = v);
            addToggle("Enchant Through Foliage", () -> cfg.enchantThroughFoliage, v -> cfg.enchantThroughFoliage = v);
            addToggle("True Vanilla Gamemode Lock", () -> cfg.trueVanillaGamemodeLock, v -> cfg.trueVanillaGamemodeLock = v);
        } else if (this.currentTab == 2) {
            addToggle("Stacking XP Orbs", () -> cfg.stackingXp, v -> cfg.stackingXp = v);
        } else if (this.currentTab == 3) {
            addGroup("MC Bugs", () -> {
                addToggle("  Ender Pearl Fix (MC-306936)", () -> cfg.mcBugs.enderPearlFix, v -> cfg.mcBugs.enderPearlFix = v);
                addToggle("  Stacked Minecart Fix (MC-14)", () -> cfg.mcBugs.stackedMinecartFix, v -> cfg.mcBugs.stackedMinecartFix = v);
                addToggle("  Rocket Elytra Speed Fix", () -> cfg.mcBugs.rocketElytraFix, v -> cfg.mcBugs.rocketElytraFix = v);
                addToggle("  Nether Portal Link Fix", () -> cfg.mcBugs.netherPortalFix, v -> cfg.mcBugs.netherPortalFix = v);
            });
            addGroup("Others", () -> {
                addToggle("  Piglin Portal Fix", () -> cfg.otherFixes.piglinPortalFix, v -> cfg.otherFixes.piglinPortalFix = v);
                addToggle("  Safe Redstone", () -> cfg.otherFixes.safeRedstone, v -> cfg.otherFixes.safeRedstone = v);
                addToggle("  No Animal Tempt Delay", () -> cfg.otherFixes.noAnimalTemptDelay, v -> cfg.otherFixes.noAnimalTemptDelay = v);
            });
        }
    }

    private void addGroup(String name, Runnable childrenBuilder) {
        boolean open = this.expandedGroups.contains(name);
        this.visibleRows.add((screen, x, y, w) -> {
            String prefix = open ? "§6▼ " : "§e▶ ";
            screen.addRenderableWidget(Button.builder(Component.literal(prefix + "§l" + name), b -> {
                if (open) screen.expandedGroups.remove(name);
                else screen.expandedGroups.add(name);
                screen.init();
            }).bounds(x, y, w, 20).build());
        });
        if (open) childrenBuilder.run();
    }

    private void addToggle(String label, BooleanSupplier getter, Consumer<Boolean> setter) {
        this.visibleRows.add((screen, x, y, w) -> {
            screen.addRenderableWidget(Button.builder(Component.literal(label), b -> {}).bounds(x, y, w - 58, 20).build());
            screen.addRenderableWidget(Button.builder(formatBool(getter.getAsBoolean()), b -> {
                boolean next = !getter.getAsBoolean();
                setter.accept(next);
                AunConfigManager.get().syncMirrors();
                b.setMessage(formatBool(next));
            }).bounds(x + w - 54, y, 54, 20).build());
        });
    }

    private void addToggleWithText(String label, BooleanSupplier getter, Consumer<Boolean> setter,
                                   Supplier<String> textGetter, Consumer<String> textSetter) {
        this.visibleRows.add((screen, x, y, w) -> {
            screen.addRenderableWidget(Button.builder(Component.literal(label), b -> {}).bounds(x, y, w - 126, 20).build());
            EditBox box = new EditBox(Minecraft.getInstance().font, x + w - 122, y + 1, 64, 18, Component.literal(label));
            box.setMaxLength(32);
            box.setValue(textGetter.get() == null ? "" : textGetter.get());
            box.setResponder(textSetter);
            screen.addRenderableWidget(box);
            screen.addRenderableWidget(Button.builder(formatBool(getter.getAsBoolean()), b -> {
                boolean next = !getter.getAsBoolean();
                setter.accept(next);
                AunConfigManager.get().syncMirrors();
                b.setMessage(formatBool(next));
            }).bounds(x + w - 54, y, 54, 20).build());
        });
    }

    private void addToggleWithTwoTexts(String label, BooleanSupplier getter, Consumer<Boolean> setter,
                                       Supplier<String> t1Get, Consumer<String> t1Set,
                                       Supplier<String> t2Get, Consumer<String> t2Set) {
        this.visibleRows.add((screen, x, y, w) -> {
            screen.addRenderableWidget(Button.builder(Component.literal(label), b -> {}).bounds(x, y, w - 154, 20).build());
            EditBox b1 = new EditBox(Minecraft.getInstance().font, x + w - 150, y + 1, 44, 18, Component.literal("1"));
            b1.setMaxLength(16);
            b1.setValue(t1Get.get() == null ? "" : t1Get.get());
            b1.setResponder(t1Set);
            screen.addRenderableWidget(b1);

            EditBox b2 = new EditBox(Minecraft.getInstance().font, x + w - 102, y + 1, 44, 18, Component.literal("2"));
            b2.setMaxLength(16);
            b2.setValue(t2Get.get() == null ? "" : t2Get.get());
            b2.setResponder(t2Set);
            screen.addRenderableWidget(b2);

            screen.addRenderableWidget(Button.builder(formatBool(getter.getAsBoolean()), b -> {
                boolean next = !getter.getAsBoolean();
                setter.accept(next);
                AunConfigManager.get().syncMirrors();
                b.setMessage(formatBool(next));
            }).bounds(x + w - 54, y, 54, 20).build());
        });
    }

    private static Component formatBool(boolean val) {
        return Component.literal(val ? "§aYES" : "§cNO");
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int delta = scrollY > 0 ? -1 : (scrollY < 0 ? 1 : 0);
        if (delta != 0) {
            int maxVisible = Math.max(1, (this.height - 66) / 22);
            int next = Math.max(0, Math.min(Math.max(0, this.visibleRows.size() - maxVisible), this.scrollOffset + delta));
            if (next != this.scrollOffset) {
                this.scrollOffset = next;
                this.init();
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void saveAndClose() {
        AunConfigManager.get().syncMirrors();
        try {
            AutoConfig.getConfigHolder(AunConfig.class).save();
        } catch (Throwable ignored) {}
        Minecraft.getInstance().setScreenAndShow(this.parent);
    }

    @Override
    public void onClose() {
        saveAndClose();
    }

    @FunctionalInterface
    private interface RowEntry {
        void buildWidgets(AunConfigScreen screen, int x, int y, int width);
    }
}
// END OF FILE
