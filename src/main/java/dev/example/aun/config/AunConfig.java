package dev.example.aun.config;

import dev.example.aun.Aun;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * Organized into four tabs:
 *   qol          = "QOL"
 *   vanillaPlus  = "Vanilla+"
 *   performance  = "Performance"
 *   fixes        = "Fixes"
 */
@Config(name = Aun.MOD_ID)
public class AunConfig implements ConfigData {

    // ============================== QOL TAB ==============================

    // A/ Clear Vision
    @ConfigEntry.Category("qol")
    @ConfigEntry.Gui.CollapsibleObject
    public ClearVisionConfig clearVision = new ClearVisionConfig();

    public static class ClearVisionConfig {
        @ConfigEntry.Gui.Tooltip public boolean clearWater = true;
        @ConfigEntry.Gui.Tooltip public boolean clearLava = true;
        @ConfigEntry.Gui.Tooltip public boolean clearPowderSnow = true;
    }

    // B/ World Setting
    @ConfigEntry.Category("qol")
    @ConfigEntry.Gui.CollapsibleObject
    public WorldSettingsConfig worldSettings = new WorldSettingsConfig();

    public static class WorldSettingsConfig {
        @ConfigEntry.Gui.Tooltip public boolean favouriteWorlds = true;
        @ConfigEntry.Gui.Tooltip public boolean worldPlaytimeDisplay = true;
        @ConfigEntry.Gui.Tooltip public boolean deletedWorldProtection = true;
    }

    // C/ Entities
    @ConfigEntry.Category("qol")
    @ConfigEntry.Gui.CollapsibleObject
    public EntitiesConfig entities = new EntitiesConfig();

    public static class EntitiesConfig {
        @ConfigEntry.Gui.Tooltip public boolean reachBehind = true;
        @ConfigEntry.Gui.Tooltip public boolean reachBehindSign = true;
        @ConfigEntry.Gui.Tooltip public boolean armorStandSwap = true;
        @ConfigEntry.Gui.Tooltip public boolean horseInfo = true;
        @ConfigEntry.Gui.Tooltip public boolean dismountOnClick = true;
    }

    // D/ Auto Setting
    @ConfigEntry.Category("qol")
    @ConfigEntry.Gui.CollapsibleObject
    public AutoSettingsConfig autoSettings = new AutoSettingsConfig();

    public static class AutoSettingsConfig {
        @ConfigEntry.Gui.Tooltip public boolean autoPickup = true;
        @ConfigEntry.Gui.Tooltip public boolean autoClimbLadders = true;
        @ConfigEntry.Gui.Tooltip public boolean autoTotem = true;
    }

    // E/ HUD
    @ConfigEntry.Category("qol")
    @ConfigEntry.Gui.CollapsibleObject
    public HudConfig hud = new HudConfig();

    public static class HudConfig {
        @ConfigEntry.Gui.Tooltip public boolean stackRefill = true;
        @ConfigEntry.Gui.Tooltip public boolean betterF1 = true;
        @ConfigEntry.Gui.Tooltip public boolean disableNarrator = true;
        @ConfigEntry.Gui.Tooltip public boolean disableAdvancementKeybind = true;
        @ConfigEntry.Gui.Tooltip public boolean ctrlDropAllSameItems = true;
        @ConfigEntry.Gui.Tooltip public boolean bundleFix = true;
        @ConfigEntry.Gui.Tooltip public boolean moreChatHistory = true;
        @ConfigEntry.Gui.Tooltip public boolean hideArmor = true;
        @ConfigEntry.Gui.Tooltip public boolean centralCrosshair = true;
        @ConfigEntry.Gui.Tooltip public boolean raisedHotbar = true;
        @ConfigEntry.Gui.Tooltip public boolean chatTextCopy = true;
        @ConfigEntry.Gui.Tooltip public boolean headLocator = true;
        @ConfigEntry.Gui.Tooltip public boolean recipeBookOrigin = true;
        @ConfigEntry.Gui.Tooltip public boolean chatHeads = true;
        @ConfigEntry.Gui.Tooltip public boolean hideOwnParticlesFirstPerson = true;
        @ConfigEntry.Gui.Tooltip public boolean hideOwnParticlesThirdPerson = false;
    }

    // Hide Nametags
    @ConfigEntry.Category("qol")
    @ConfigEntry.Gui.CollapsibleObject
    public HideNametagsConfig hideNametags = new HideNametagsConfig();

    public static class HideNametagsConfig {
        @ConfigEntry.Gui.Tooltip public boolean hidePlayerNametags = false;
        @ConfigEntry.Gui.Tooltip public boolean hideMobNametags = false;
        @ConfigEntry.Gui.Tooltip public String hiddenMobsList = "";
    }

    // Other QOL Toggles
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean cutThrough = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean doubleDoors = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean simpleHarvest = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean fastLeafDecay = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean closeCall = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean autoToolSwitch = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean bookScroll = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean fullbright = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean netherCalc = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean noAccidentalLogStripping = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean zoom = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean facingPlacement = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean accurateBlockPlacement = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean borderlessFullscreen = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean noResourcePackWarning = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean enchantmentDescriptions = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean dynamicLights = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean fadelessLoading = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean fastQuit = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean fastLanguageReload = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean chestSearchBar = true;

    // =========================== VANILLA+ TAB ============================

    // A/ Griefing Protection
    @ConfigEntry.Category("vanillaPlus")
    @ConfigEntry.Gui.CollapsibleObject
    public MobGriefingConfig mobGriefing = new MobGriefingConfig();

    public static class MobGriefingConfig {
        @ConfigEntry.Gui.Tooltip public boolean antiEndermanPickup = true;
        @ConfigEntry.Gui.Tooltip public boolean antiCreeperGrief = true;
        @ConfigEntry.Gui.Tooltip public boolean antiGhastGrief = true;
        @ConfigEntry.Gui.Tooltip public boolean antiBlazeGrief = true;
        @ConfigEntry.Gui.Tooltip public boolean antiRavagerGrief = true;
        @ConfigEntry.Gui.Tooltip public boolean safeLightning = true;
        @ConfigEntry.Gui.Tooltip public boolean noZombieDoorBreak = true;
        @ConfigEntry.Gui.Tooltip public boolean noCropTrampleFeatherFalling = true;
        @ConfigEntry.Gui.Tooltip public boolean noCropTrampleMobs = true;
    }

    // B/ Insta-Mine
    @ConfigEntry.Category("vanillaPlus")
    @ConfigEntry.Gui.CollapsibleObject
    public InstaMineConfig instaMine = new InstaMineConfig();

    public static class InstaMineConfig {
        @ConfigEntry.Gui.Tooltip public boolean instaMineGlass = true;
        @ConfigEntry.Gui.Tooltip public boolean instaMineDeepslateWithHaste = true;
    }

    // Other Vanilla+ Toggles
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean hideBeaconBeamBehindTintedGlass = true;
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean speedHappyGhast = true;
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public String happyGhastBoost = "100";
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean noBatSpawning = true;
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean anvilNoTooExpensive = true;
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean playerDropsNeverDespawn = true;
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean infiniteVillagerTrading = true;
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean enchantThroughFoliage = true;
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean trueVanillaGamemodeLock = false;

    // ========================== PERFORMANCE TAB ==========================

    @ConfigEntry.Category("performance") @ConfigEntry.Gui.Tooltip public boolean stackingXp = true;

    // ============================= FIXES TAB =============================

    // A/ MC Bugs
    @ConfigEntry.Category("fixes")
    @ConfigEntry.Gui.CollapsibleObject
    public McBugsConfig mcBugs = new McBugsConfig();

    public static class McBugsConfig {
        @ConfigEntry.Gui.Tooltip public boolean enderPearlFix = true;
        @ConfigEntry.Gui.Tooltip public boolean stackedMinecartFix = true;
        @ConfigEntry.Gui.Tooltip public boolean rocketElytraFix = true;
        @ConfigEntry.Gui.Tooltip public boolean netherPortalFix = true;
    }

    // B/ Others
    @ConfigEntry.Category("fixes")
    @ConfigEntry.Gui.CollapsibleObject
    public OtherFixesConfig otherFixes = new OtherFixesConfig();

    public static class OtherFixesConfig {
        @ConfigEntry.Gui.Tooltip public boolean piglinPortalFix = true;
        @ConfigEntry.Gui.Tooltip public boolean safeRedstone = true;
        @ConfigEntry.Gui.Tooltip public boolean noAnimalTemptDelay = true;
    }

    // =================== HIDDEN MIRRORS FOR CODE COMPATIBILITY ===================
    @ConfigEntry.Gui.Excluded public transient boolean clearWater = true;
    @ConfigEntry.Gui.Excluded public transient boolean clearLava = true;
    @ConfigEntry.Gui.Excluded public transient boolean clearPowderSnow = true;

    @ConfigEntry.Gui.Excluded public transient boolean favouriteWorlds = true;
    @ConfigEntry.Gui.Excluded public transient boolean worldPlaytimeDisplay = true;
    @ConfigEntry.Gui.Excluded public transient boolean deletedWorldProtection = true;

    @ConfigEntry.Gui.Excluded public transient boolean reachBehind = true;
    @ConfigEntry.Gui.Excluded public transient boolean reachBehindSign = true;
    @ConfigEntry.Gui.Excluded public transient boolean armorStandSwap = true;
    @ConfigEntry.Gui.Excluded public transient boolean horseInfo = true;
    @ConfigEntry.Gui.Excluded public transient boolean dismountOnClick = true;

    @ConfigEntry.Gui.Excluded public transient boolean autoPickup = true;
    @ConfigEntry.Gui.Excluded public transient boolean autoClimbLadders = true;
    @ConfigEntry.Gui.Excluded public transient boolean autoTotem = true;

    @ConfigEntry.Gui.Excluded public transient boolean stackRefill = true;
    @ConfigEntry.Gui.Excluded public transient boolean betterF1 = true;
    @ConfigEntry.Gui.Excluded public transient boolean disableNarrator = true;
    @ConfigEntry.Gui.Excluded public transient boolean disableAdvancementKeybind = true;
    @ConfigEntry.Gui.Excluded public transient boolean ctrlDropAllSameItems = true;
    @ConfigEntry.Gui.Excluded public transient boolean bundleFix = true;
    @ConfigEntry.Gui.Excluded public transient boolean moreChatHistory = true;
    @ConfigEntry.Gui.Excluded public transient boolean hideArmor = true;
    @ConfigEntry.Gui.Excluded public transient boolean centralCrosshair = true;
    @ConfigEntry.Gui.Excluded public transient boolean raisedHotbar = true;
    @ConfigEntry.Gui.Excluded public transient boolean chatTextCopy = true;
    @ConfigEntry.Gui.Excluded public transient boolean headLocator = true;
    @ConfigEntry.Gui.Excluded public transient boolean recipeBookOrigin = true;
    @ConfigEntry.Gui.Excluded public transient boolean chatHeads = true;
    @ConfigEntry.Gui.Excluded public transient boolean hideOwnParticlesFirstPerson = true;
    @ConfigEntry.Gui.Excluded public transient boolean hideOwnParticlesThirdPerson = false;

    @ConfigEntry.Gui.Excluded public transient boolean safeLightning = true;
    @ConfigEntry.Gui.Excluded public transient boolean noZombieDoorBreak = true;
    @ConfigEntry.Gui.Excluded public transient boolean noCropTrampleFeatherFalling = true;
    @ConfigEntry.Gui.Excluded public transient boolean noCropTrampleMobs = true;

    @ConfigEntry.Gui.Excluded public transient boolean instaMineGlass = true;
    @ConfigEntry.Gui.Excluded public transient boolean instaMineDeepslateWithHaste = true;

    @ConfigEntry.Gui.Excluded public transient boolean enderPearlFix = true;
    @ConfigEntry.Gui.Excluded public transient boolean stackedMinecartFix = true;
    @ConfigEntry.Gui.Excluded public transient boolean rocketElytraFix = true;
    @ConfigEntry.Gui.Excluded public transient boolean netherPortalFix = true;

    @ConfigEntry.Gui.Excluded public transient boolean piglinPortalFix = true;
    @ConfigEntry.Gui.Excluded public transient boolean safeRedstone = true;
    @ConfigEntry.Gui.Excluded public transient boolean noAnimalTemptDelay = true;

    @Override
    public void validatePostLoad() {
        syncMirrors();
    }

    public void syncMirrors() {
        if (clearVision != null) {
            this.clearWater = clearVision.clearWater;
            this.clearLava = clearVision.clearLava;
            this.clearPowderSnow = clearVision.clearPowderSnow;
        }
        if (worldSettings != null) {
            this.favouriteWorlds = worldSettings.favouriteWorlds;
            this.worldPlaytimeDisplay = worldSettings.worldPlaytimeDisplay;
            this.deletedWorldProtection = worldSettings.deletedWorldProtection;
        }
        if (entities != null) {
            this.reachBehind = entities.reachBehind;
            this.reachBehindSign = entities.reachBehindSign;
            this.armorStandSwap = entities.armorStandSwap;
            this.horseInfo = entities.horseInfo;
            this.dismountOnClick = entities.dismountOnClick;
        }
        if (autoSettings != null) {
            this.autoPickup = autoSettings.autoPickup;
            this.autoClimbLadders = autoSettings.autoClimbLadders;
            this.autoTotem = autoSettings.autoTotem;
        }
        if (hud != null) {
            this.stackRefill = hud.stackRefill;
            this.betterF1 = hud.betterF1;
            this.disableNarrator = hud.disableNarrator;
            this.disableAdvancementKeybind = hud.disableAdvancementKeybind;
            this.ctrlDropAllSameItems = hud.ctrlDropAllSameItems;
            this.bundleFix = hud.bundleFix;
            this.moreChatHistory = hud.moreChatHistory;
            this.hideArmor = hud.hideArmor;
            this.centralCrosshair = hud.centralCrosshair;
            this.raisedHotbar = hud.raisedHotbar;
            this.chatTextCopy = hud.chatTextCopy;
            this.headLocator = hud.headLocator;
            this.recipeBookOrigin = hud.recipeBookOrigin;
            this.chatHeads = hud.chatHeads;
            this.hideOwnParticlesFirstPerson = hud.hideOwnParticlesFirstPerson;
            this.hideOwnParticlesThirdPerson = hud.hideOwnParticlesThirdPerson;
        }
        if (mobGriefing != null) {
            this.safeLightning = mobGriefing.safeLightning;
            this.noZombieDoorBreak = mobGriefing.noZombieDoorBreak;
            this.noCropTrampleFeatherFalling = mobGriefing.noCropTrampleFeatherFalling;
            this.noCropTrampleMobs = mobGriefing.noCropTrampleMobs;
        }
        if (instaMine != null) {
            this.instaMineGlass = instaMine.instaMineGlass;
            this.instaMineDeepslateWithHaste = instaMine.instaMineDeepslateWithHaste;
        }
        if (mcBugs != null) {
            this.enderPearlFix = mcBugs.enderPearlFix;
            this.stackedMinecartFix = mcBugs.stackedMinecartFix;
            this.rocketElytraFix = mcBugs.rocketElytraFix;
            this.netherPortalFix = mcBugs.netherPortalFix;
        }
        if (otherFixes != null) {
            this.piglinPortalFix = otherFixes.piglinPortalFix;
            this.safeRedstone = otherFixes.safeRedstone;
            this.noAnimalTemptDelay = otherFixes.noAnimalTemptDelay;
        }
    }
}
