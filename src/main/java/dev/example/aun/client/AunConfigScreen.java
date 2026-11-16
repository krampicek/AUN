package dev.example.aun.client;

import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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

        String[] tabs = {"General", "QOL", "Vanilla+", "Performance", "Fixes"};
        int tabWidth = 64;
        int startX = (this.width - (tabs.length * (tabWidth + 3))) / 2;
        for (int i = 0; i < tabs.length; i++) {
            final int tabIdx = i;
            String label = (this.currentTab == i ? "§e§l" : "§7") + tabs[i];
            this.addRenderableWidget(Button.builder(Component.literal(label), btn -> {
                this.currentTab = tabIdx;
                this.scrollOffset = 0;
                this.init();
            }).bounds(startX + i * (tabWidth + 3), 8, tabWidth, 20).build());
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

        if (this.visibleRows.size() > maxVisible) {
            this.addRenderableWidget(Button.builder(Component.literal("▲"), b -> {
                if (this.scrollOffset > 0) { this.scrollOffset--; this.init(); }
            }).bounds(rowLeft + rowWidth + 6, listTop, 20, 20).build());

            this.addRenderableWidget(Button.builder(Component.literal("▼"), b -> {
                if (this.scrollOffset < this.visibleRows.size() - maxVisible) { this.scrollOffset++; this.init(); }
            }).bounds(rowLeft + rowWidth + 6, listBottom - 20, 20, 20).build());
        }

        // Standalone Save Button (Does not close screen)
        this.addRenderableWidget(Button.builder(Component.literal("§6Save"), btn -> {
            saveConfigToDisk();
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendOverlayMessage(Component.literal("§aA-U-N settings saved to disk!"));
            }
        }).bounds(this.width / 2 - 124, this.height - 26, 58, 20).build());

        // Save & Done Button (Saves and exits)
        this.addRenderableWidget(Button.builder(Component.literal("§aSave & Done"), btn -> {
            saveAndClose();
        }).bounds(this.width / 2 - 62, this.height - 26, 124, 20).build());

        // Export Own Profile Button
        this.addRenderableWidget(Button.builder(Component.literal("§bExport Profile"), btn -> {
            exportOwnProfile();
        }).bounds(this.width / 2 + 66, this.height - 26, 88, 20)
        .tooltip(Tooltip.create(Component.literal("Exports your configuration profile and opens the folder.")))
        .build());
    }

    private void buildRowsForCurrentTab(AunConfig cfg) {
        if (this.currentTab == 0) {
            // GENERAL TAB
            addToggleWithText("Toggle AUN Features", () -> cfg.general.modEnabled, v -> cfg.general.modEnabled = v,
                    () -> cfg.general.toggleKey, s -> cfg.general.toggleKey = s);

            addCustomDropdownRow("Settings Profile", cfg);

            if ("own".equalsIgnoreCase(cfg.general.profileMode)) {
                addTextRow("  Link Own Profile", () -> cfg.general.customProfilePath, s -> cfg.general.customProfilePath = s);
            }
        } else if (this.currentTab == 1) {
            // QOL TAB
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
                addToggle("  Auto Save Your Stuff", () -> cfg.autoSettings.closeCall, v -> cfg.autoSettings.closeCall = v);
                addToggle("  Auto Switch Tools", () -> cfg.autoSettings.autoToolSwitch, v -> cfg.autoSettings.autoToolSwitch = v);
                addToggle("  Auto Harvest", () -> cfg.autoSettings.simpleHarvest, v -> cfg.autoSettings.simpleHarvest = v);
            });
            addGroup("Player HUD", () -> {
                addToggle("  Stack Refill", () -> cfg.playerHud.stackRefill, v -> cfg.playerHud.stackRefill = v);
                addToggle("  Better F1", () -> cfg.playerHud.betterF1, v -> cfg.playerHud.betterF1 = v);
                addToggle("  Expandable Bundle Preview", () -> cfg.playerHud.bundleFix, v -> cfg.playerHud.bundleFix = v);
                addToggle("  Hide Armor (Eye Toggle)", () -> cfg.playerHud.hideArmor, v -> cfg.playerHud.hideArmor = v);
                addToggle("  Raised Hotbar (+2px Gap)", () -> cfg.playerHud.raisedHotbar, v -> cfg.playerHud.raisedHotbar = v);
                addToggle("  Head Locator HUD", () -> cfg.playerHud.headLocator, v -> cfg.playerHud.headLocator = v);
                addToggle("  Locator Bar Combination", () -> cfg.playerHud.locatorBarCombination, v -> cfg.playerHud.locatorBarCombination = v);
                addToggle("  Chat Heads", () -> cfg.playerHud.chatHeads, v -> cfg.playerHud.chatHeads = v);
                addToggle("  Hide Own Particles (1st)", () -> cfg.playerHud.hideOwnParticlesFirstPerson, v -> cfg.playerHud.hideOwnParticlesFirstPerson = v);
                addToggle("  Hide Own Particles (3rd)", () -> cfg.playerHud.hideOwnParticlesThirdPerson, v -> cfg.playerHud.hideOwnParticlesThirdPerson = v);
                addToggle("  Central Crosshair", () -> cfg.playerHud.centralCrosshair, v -> cfg.playerHud.centralCrosshair = v);
            });
            addGroup("Remainings HUDs", () -> {
                addToggle("  Disable Vanilla Narrator", () -> cfg.remainingHuds.disableNarrator, v -> cfg.remainingHuds.disableNarrator = v);
                addToggle("  Disable Advancements Key", () -> cfg.remainingHuds.disableAdvancementKeybind, v -> cfg.remainingHuds.disableAdvancementKeybind = v);
                addToggle("  Sprint + Q Drop All Same", () -> cfg.remainingHuds.ctrlDropAllSameItems, v -> cfg.remainingHuds.ctrlDropAllSameItems = v);
                addToggle("  More Chat History (1000)", () -> cfg.remainingHuds.moreChatHistory, v -> cfg.remainingHuds.moreChatHistory = v);
                addToggle("  Chat Text Highlight/Copy", () -> cfg.remainingHuds.chatTextCopy, v -> cfg.remainingHuds.chatTextCopy = v);
                addToggle("  Recipe Book Origin Link", () -> cfg.remainingHuds.recipeBookOrigin, v -> cfg.remainingHuds.recipeBookOrigin = v);
                addToggle("  Book Scroll", () -> cfg.remainingHuds.bookScroll, v -> cfg.remainingHuds.bookScroll = v);
                addToggle("  Zoom (Smooth + Wheel)", () -> cfg.remainingHuds.zoom, v -> cfg.remainingHuds.zoom = v);
                addToggle("  Dynamic Held Lights", () -> cfg.remainingHuds.dynamicLights, v -> cfg.remainingHuds.dynamicLights = v);
                addToggle("  Search Bar In Chests", () -> cfg.remainingHuds.chestSearchBar, v -> cfg.remainingHuds.chestSearchBar = v);
            });
            addGroup("Commands", () -> {
                addToggle("  /nethercalc Command", () -> cfg.commands.netherCalc, v -> cfg.commands.netherCalc = v);
            });
            addGroup("Screen", () -> {
                addToggle("  No Resource Pack Warning", () -> cfg.screenSettings.noResourcePackWarning, v -> cfg.screenSettings.noResourcePackWarning = v);
                addToggle("  Fast Language Reload", () -> cfg.screenSettings.fastLanguageReload, v -> cfg.screenSettings.fastLanguageReload = v);
                addToggle("  Fast World Quit", () -> cfg.screenSettings.fastQuit, v -> cfg.screenSettings.fastQuit = v);
                addToggle("  Fadeless Loading Screen", () -> cfg.screenSettings.fadelessLoading, v -> cfg.screenSettings.fadelessLoading = v);
                addToggle("  Borderless Fullscreen", () -> cfg.screenSettings.borderlessFullscreen, v -> cfg.screenSettings.borderlessFullscreen = v);
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
            addToggleWithTwoTexts("Directional Placement", () -> cfg.facingPlacement, v -> cfg.facingPlacement = v,
                    () -> cfg.facingIntoKey, s -> cfg.facingIntoKey = s, () -> cfg.facingAwayKey, s -> cfg.facingAwayKey = s);

            addToggle("Cut Through Tall Grass", () -> cfg.cutThrough, v -> cfg.cutThrough = v);
            addToggle("Double Doors", () -> cfg.doubleDoors, v -> cfg.doubleDoors = v);
            addToggle("Fast Leaf Decay", () -> cfg.fastLeafDecay, v -> cfg.fastLeafDecay = v);
            addToggle("Prevent Accidental Log Strip", () -> cfg.noAccidentalLogStripping, v -> cfg.noAccidentalLogStripping = v);
            addToggle("Accurate Block Placement", () -> cfg.accurateBlockPlacement, v -> cfg.accurateBlockPlacement = v);
            addToggle("Enchantment Descriptions", () -> cfg.enchantmentDescriptions, v -> cfg.enchantmentDescriptions = v);
        } else if (this.currentTab == 2) {
            // VANILLA+ TAB
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
            addGroup("Happy Ghast", () -> {
                addToggle("  Speed Boost For Happy Ghast", () -> cfg.happyGhast.speedHappyGhast, v -> cfg.happyGhast.speedHappyGhast = v);
                addToggleWithText("  Horizontal Speed Boost", () -> cfg.happyGhast.horizontalSpeedEnabled, v -> cfg.happyGhast.horizontalSpeedEnabled = v,
                        () -> cfg.happyGhast.happyGhastHorizontalBoost, s -> cfg.happyGhast.happyGhastHorizontalBoost = s);
                addToggleWithText("  Vertical Speed Boost", () -> cfg.happyGhast.verticalSpeedEnabled, v -> cfg.happyGhast.verticalSpeedEnabled = v,
                        () -> cfg.happyGhast.happyGhastVerticalBoost, s -> cfg.happyGhast.happyGhastVerticalBoost = s);
            });
            addGroup("Essential", () -> {
                addToggle("  Glowing Trims", () -> cfg.essential.glowingTrims, v -> cfg.essential.glowingTrims = v);
                addToggle("  Coral Reborn", () -> cfg.essential.coralReborn, v -> cfg.essential.coralReborn = v);
                addToggle("  More Dyes Recipes", () -> cfg.essential.moreDyes, v -> cfg.essential.moreDyes = v);
                addToggle("  Tinted Glass Hides Beacon", () -> cfg.essential.hideBeaconBeamBehindTintedGlass, v -> cfg.essential.hideBeaconBeamBehindTintedGlass = v);
                addToggle("  No Bats", () -> cfg.essential.noBatSpawning, v -> cfg.essential.noBatSpawning = v);
                addToggle("  No \"Too Expensive!\" Anvil", () -> cfg.essential.anvilNoTooExpensive, v -> cfg.essential.anvilNoTooExpensive = v);
                addToggle("  Player Drops Never Despawn", () -> cfg.essential.playerDropsNeverDespawn, v -> cfg.essential.playerDropsNeverDespawn = v);
                addToggle("  Infinite Villager Trading", () -> cfg.essential.infiniteVillagerTrading, v -> cfg.essential.infiniteVillagerTrading = v);
                addToggle("  Enchant Through Foliage", () -> cfg.essential.enchantThroughFoliage, v -> cfg.essential.enchantThroughFoliage = v);
                addToggle("  True Vanilla Gamemode Lock", () -> cfg.essential.trueVanillaGamemodeLock, v -> cfg.essential.trueVanillaGamemodeLock = v);
            });
        } else if (this.currentTab == 3) {
            // PERFORMANCE TAB
            addToggle("Stacking XP Orbs", () -> cfg.stackingXp, v -> cfg.stackingXp = v);
        } else if (this.currentTab == 4) {
            // FIXES TAB
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

    private void addCustomDropdownRow(String label, AunConfig cfg) {
        this.visibleRows.add((screen, x, y, w) -> {
            screen.addRenderableWidget(Button.builder(Component.literal(label), b -> {}).bounds(x, y, w - 110, 20).build());
            String mode = cfg.general.profileMode == null ? "default" : cfg.general.profileMode;
            String display = switch (mode.toLowerCase(Locale.ROOT)) {
                case "on" -> "§aALL ON";
                case "off" -> "§cALL OFF";
                case "own" -> "§bOWN PROFILE";
                default -> "§eDEFAULT";
            };
            screen.addRenderableWidget(Button.builder(Component.literal(display), b -> {
                String next = switch (mode.toLowerCase(Locale.ROOT)) {
                    case "default" -> "on";
                    case "on" -> "off";
                    case "off" -> "own";
                    default -> "default";
                };
                cfg.general.profileMode = next;
                if ("on".equals(next)) applyProfileAll(cfg, true);
                else if ("off".equals(next)) applyProfileAll(cfg, false);
                screen.init();
            }).bounds(x + w - 106, y, 106, 20).build());
        });
    }

    private void addTextRow(String label, Supplier<String> getter, Consumer<String> setter) {
        this.visibleRows.add((screen, x, y, w) -> {
            screen.addRenderableWidget(Button.builder(Component.literal(label), b -> {}).bounds(x, y, w - 190, 20).build());
            EditBox box = new EditBox(Minecraft.getInstance().font, x + w - 186, y + 1, 182, 18, Component.literal(label));
            box.setMaxLength(256);
            box.setValue(getter.get() == null ? "" : getter.get());
            box.setResponder(setter);
            screen.addRenderableWidget(box);
        });
    }

    private static void applyProfileAll(AunConfig cfg, boolean val) {
        cfg.clearVision.clearWater = val;
        cfg.clearVision.clearLava = val;
        cfg.clearVision.clearPowderSnow = val;
        cfg.worldSettings.favouriteWorlds = val;
        cfg.worldSettings.worldPlaytimeDisplay = val;
        cfg.worldSettings.deletedWorldProtection = val;
        cfg.entities.reachBehind = val;
        cfg.entities.reachBehindSign = val;
        cfg.entities.armorStandSwap = val;
        cfg.entities.horseInfo = false;
        cfg.entities.dismountOnClick = val;
        cfg.autoSettings.autoPickup = val;
        cfg.autoSettings.autoClimbLadders = val;
        cfg.autoSettings.autoTotem = val;
        cfg.autoSettings.closeCall = val;
        cfg.autoSettings.autoToolSwitch = val;
        cfg.autoSettings.simpleHarvest = val;
        cfg.playerHud.stackRefill = val;
        cfg.playerHud.betterF1 = val;
        cfg.playerHud.bundleFix = val;
        cfg.playerHud.hideArmor = val;
        cfg.playerHud.raisedHotbar = val;
        cfg.playerHud.headLocator = val;
        cfg.playerHud.locatorBarCombination = val;
        cfg.playerHud.chatHeads = val;
        cfg.playerHud.hideOwnParticlesFirstPerson = val;
        cfg.playerHud.hideOwnParticlesThirdPerson = val;
        cfg.playerHud.centralCrosshair = val;
        cfg.remainingHuds.disableNarrator = val;
        cfg.remainingHuds.disableAdvancementKeybind = val;
        cfg.remainingHuds.ctrlDropAllSameItems = val;
        cfg.remainingHuds.moreChatHistory = val;
        cfg.remainingHuds.chatTextCopy = val;
        cfg.remainingHuds.recipeBookOrigin = val;
        cfg.remainingHuds.bookScroll = val;
        cfg.remainingHuds.zoom = val;
        cfg.remainingHuds.dynamicLights = val;
        cfg.remainingHuds.chestSearchBar = val;
        cfg.commands.netherCalc = val;
        cfg.screenSettings.noResourcePackWarning = val;
        cfg.screenSettings.fastLanguageReload = val;
        cfg.screenSettings.fastQuit = val;
        cfg.screenSettings.fadelessLoading = val;
        cfg.screenSettings.borderlessFullscreen = val;
        cfg.fullbright = val;
        cfg.facingPlacement = val;
        cfg.noAccidentalLogStripping = val;
        cfg.accurateBlockPlacement = val;
        cfg.enchantmentDescriptions = val;
        cfg.mobGriefing.antiEndermanPickup = val;
        cfg.mobGriefing.antiCreeperGrief = val;
        cfg.mobGriefing.antiGhastGrief = val;
        cfg.mobGriefing.antiBlazeGrief = val;
        cfg.mobGriefing.antiRavagerGrief = val;
        cfg.mobGriefing.safeLightning = val;
        cfg.mobGriefing.noZombieDoorBreak = val;
        cfg.mobGriefing.noCropTrampleFeatherFalling = val;
        cfg.mobGriefing.noCropTrampleMobs = val;
        cfg.instaMine.instaMineGlass = val;
        cfg.instaMine.instaMineDeepslateWithHaste = val;
        cfg.happyGhast.speedHappyGhast = val;
        cfg.happyGhast.horizontalSpeedEnabled = val;
        cfg.happyGhast.verticalSpeedEnabled = val;
        cfg.essential.glowingTrims = val;
        cfg.essential.coralReborn = val;
        cfg.essential.moreDyes = val;
        cfg.essential.hideBeaconBeamBehindTintedGlass = val;
        cfg.essential.noBatSpawning = val;
        cfg.essential.anvilNoTooExpensive = val;
        cfg.essential.playerDropsNeverDespawn = val;
        cfg.essential.infiniteVillagerTrading = val;
        cfg.essential.enchantThroughFoliage = val;
        cfg.essential.trueVanillaGamemodeLock = val;
        cfg.stackingXp = val;
        cfg.mcBugs.enderPearlFix = val;
        cfg.mcBugs.stackedMinecartFix = val;
        cfg.mcBugs.rocketElytraFix = val;
        cfg.mcBugs.netherPortalFix = val;
        cfg.otherFixes.piglinPortalFix = val;
        cfg.otherFixes.safeRedstone = val;
        cfg.otherFixes.noAnimalTemptDelay = val;
        cfg.syncMirrors();
    }

    private void exportOwnProfile() {
        try {
            saveConfigToDisk();
            Path gameDir = FabricLoader.getInstance().getGameDir();
            Path exportPath = gameDir.resolve("aun_exported_profile.json");
            Path configPath = FabricLoader.getInstance().getConfigDir().resolve("aun.json");
            if (Files.exists(configPath)) {
                Files.copy(configPath, exportPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(gameDir.toFile());
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.sendOverlayMessage(Component.literal("§aExported profile to game directory!"));
            }
        } catch (Throwable ignored) {}
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

    private void saveConfigToDisk() {
        AunConfigManager.get().syncMirrors();
        try {
            AutoConfig.getConfigHolder(AunConfig.class).save();
        } catch (Throwable ignored) {}
    }

    private void saveAndClose() {
        saveConfigToDisk();
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
