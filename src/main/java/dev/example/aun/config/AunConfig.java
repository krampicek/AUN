package dev.example.aun.config;

import dev.example.aun.Aun;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = Aun.MOD_ID)
public class AunConfig implements ConfigData {

    // ============================== QOL TAB ==============================
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.CollapsibleObject
    public ClearVisionConfig clearVision = new ClearVisionConfig();
    public static class ClearVisionConfig {
        @ConfigEntry.Gui.Tooltip public boolean clearWater = true;
        @ConfigEntry.Gui.Tooltip public boolean clearLava = true;
        @ConfigEntry.Gui.Tooltip public boolean clearPowderSnow = true;
    }

    @ConfigEntry.Category("qol") @ConfigEntry.Gui.CollapsibleObject
    public WorldSettingsConfig worldSettings = new WorldSettingsConfig();
    public static class WorldSettingsConfig {
        @ConfigEntry.Gui.Tooltip public boolean favouriteWorlds = true;
        @ConfigEntry.Gui.Tooltip public boolean worldPlaytimeDisplay = true;
        @ConfigEntry.Gui.Tooltip public boolean deletedWorldProtection = true;
    }

    @ConfigEntry.Category("qol") @ConfigEntry.Gui.CollapsibleObject
    public EntitiesConfig entities = new EntitiesConfig();
    public static class EntitiesConfig {
        @ConfigEntry.Gui.Tooltip public boolean reachBehind = true;
        @ConfigEntry.Gui.Tooltip public boolean reachBehindSign = true;
        @ConfigEntry.Gui.Tooltip public boolean armorStandSwap = true;
        @ConfigEntry.Gui.Tooltip public boolean horseInfo = true;
        @ConfigEntry.Gui.Tooltip public boolean dismountOnClick = true;
    }

    @ConfigEntry.Category("qol") @ConfigEntry.Gui.CollapsibleObject
    public AutoSettingsConfig autoSettings = new AutoSettingsConfig();
    public static class AutoSettingsConfig {
        @ConfigEntry.Gui.Tooltip public boolean autoPickup = true;
        @ConfigEntry.Gui.Tooltip public boolean autoClimbLadders = true;
        @ConfigEntry.Gui.Tooltip public boolean autoTotem = true;
    }

    @ConfigEntry.Category("qol") @ConfigEntry.Gui.CollapsibleObject
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

    @ConfigEntry.Category("qol") @ConfigEntry.Gui.CollapsibleObject
    public HideNametagsConfig hideNametags = new HideNametagsConfig();
    public static class HideNametagsConfig {
        @ConfigEntry.Gui.Tooltip public boolean hidePlayerNametags = false;
        @ConfigEntry.Gui.Tooltip public boolean hideMobNametags = false;
        @ConfigEntry.Gui.Tooltip public boolean hideHostileMobNametags = false;
        @ConfigEntry.Gui.Tooltip public boolean hidePassiveMobNametags = false;
        @ConfigEntry.Gui.Tooltip public boolean hideTamedMobNametags = false;
        @ConfigEntry.Gui.Tooltip public String hiddenMobsList = "";
    }

    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean cutThrough = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean doubleDoors = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean simpleHarvest = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean fastLeafDecay = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean closeCall = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean autoToolSwitch = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean bookScroll = true;

    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean fullbright = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public String fullbrightKey = "H";

    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean zoom = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public String zoomKey = "C";

    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean facingPlacement = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public String facingIntoKey = "V";
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public String facingAwayKey = "LCONTROL";

    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean netherCalc = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean noAccidentalLogStripping = true;
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
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.CollapsibleObject
    public MobGriefingConfig mobGriefing = new MobGriefingConfig();
    public static class MobGriefingConfig {
        @ConfigEntry.Gui.Tooltip public boolean antiEndermanPickup = true;
        @ConfigEntry.Gui.Tooltip public boolean antiCreeperGrief = true;
        @ConfigEntry.Gui.Tooltip public boolean antiGhastGrief = true;
        @ConfigEntry.Gui.Tooltip public boolean antiBlazeGrief = true;
        @ConfigEntry.Gui.Tooltip public boolean antiRavagerGrief = true;
        @ConfigEntry.Gui.Tooltip public boolean safeLightning = true;
        @ConfigEntry.Gui.Tooltip public boolean noZombieDoorBreak = true;
        @ConfigEntry.Gui.Tooltip public boolean noCropTrampleFeatherFalling =
