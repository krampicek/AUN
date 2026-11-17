package dev.example.aun.config;

import dev.example.aun.Aun;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = Aun.MOD_ID)
public class AunConfig implements ConfigData {

    public enum ProfileMode {
        DEFAULT,
        ALL_ON,
        ALL_OFF,
        OWN_PROFILE
    }

    // ============================== GENERAL TAB ==============================
    @ConfigEntry.Category("general") @ConfigEntry.Gui.TransitiveObject
    public GeneralSettingsConfig general = new GeneralSettingsConfig();
    public static class GeneralSettingsConfig {
        @ConfigEntry.Gui.Tooltip public boolean modEnabled = true;
        @ConfigEntry.Gui.Tooltip public String toggleKey = "N";
        @ConfigEntry.Gui.Tooltip public boolean kindaCheaty = false;
        @ConfigEntry.Gui.Tooltip @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public ProfileMode profileMode = ProfileMode.DEFAULT;
        @ConfigEntry.Gui.Tooltip public String customProfilePath = "";
    }

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
        @ConfigEntry.Gui.Tooltip public boolean closeCall = true;
        @ConfigEntry.Gui.Tooltip public boolean autoToolSwitch = true;
        @ConfigEntry.Gui.Tooltip public boolean simpleHarvest = true;
    }

    @ConfigEntry.Category("qol") @ConfigEntry.Gui.CollapsibleObject
    public PlayerHudConfig playerHud = new PlayerHudConfig();
    public static class PlayerHudConfig {
        @ConfigEntry.Gui.Tooltip public boolean stackRefill = true;
        @ConfigEntry.Gui.Tooltip public boolean betterF1 = true;
        @ConfigEntry.Gui.Tooltip public boolean bundleFix = true;
        @ConfigEntry.Gui.Tooltip public boolean hideArmor = true;
        @ConfigEntry.Gui.Tooltip public boolean raisedHotbar = true;
        @ConfigEntry.Gui.Tooltip public boolean headLocator = true;
        @ConfigEntry.Gui.Tooltip public boolean locatorBarCombination = true;
        @ConfigEntry.Gui.Tooltip public boolean chatHeads = true;
        @ConfigEntry.Gui.Tooltip public boolean hideOwnParticlesFirstPerson = true;
        @ConfigEntry.Gui.Tooltip public boolean hideOwnParticlesThirdPerson = true;
        @ConfigEntry.Gui.Tooltip public boolean centralCrosshair = true;
    }

    @ConfigEntry.Category("qol") @ConfigEntry.Gui.CollapsibleObject
    public RemainingHudsConfig remainingHuds = new RemainingHudsConfig();
    public static class RemainingHudsConfig {
        @ConfigEntry.Gui.Tooltip public boolean disableNarrator = true;
        @ConfigEntry.Gui.Tooltip public boolean disableAdvancementKeybind = true;
        @ConfigEntry.Gui.Tooltip public boolean ctrlDropAllSameItems = true;
        @ConfigEntry.Gui.Tooltip public boolean moreChatHistory = true;
        @ConfigEntry.Gui.Tooltip public boolean chatTextCopy = true;
        @ConfigEntry.Gui.Tooltip public boolean recipeBookOrigin = true;
        @ConfigEntry.Gui.Tooltip public boolean bookScroll = true;
        @ConfigEntry.Gui.Tooltip public boolean zoom = true;
        @ConfigEntry.Gui.Tooltip public String zoomKey = "C";
        @ConfigEntry.Gui.Tooltip public boolean dynamicLights = true;
        @ConfigEntry.Gui.Tooltip public boolean chestSearchBar = true;
    }

    @ConfigEntry.Category("qol") @ConfigEntry.Gui.CollapsibleObject
    public CommandsConfig commands = new CommandsConfig();
    public static class CommandsConfig {
        @ConfigEntry.Gui.Tooltip public boolean netherCalc = true;
    }

    @ConfigEntry.Category("qol") @ConfigEntry.Gui.CollapsibleObject
    public ScreenConfig screenSettings = new ScreenConfig();
    public static class ScreenConfig {
        @ConfigEntry.Gui.Tooltip public boolean noResourcePackWarning = true;
        @ConfigEntry.Gui.Tooltip public boolean fastLanguageReload = true;
        @ConfigEntry.Gui.Tooltip public boolean fastQuit = true;
        @ConfigEntry.Gui.Tooltip public boolean fadelessLoading = true;
        @ConfigEntry.Gui.Tooltip public boolean borderlessFullscreen = true;
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
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean fastLeafDecay = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean fullbright = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public String fullbrightKey = "H";
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean facingPlacement = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public String facingIntoKey = "V";
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public String facingAwayKey = "LCONTROL";
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean noAccidentalLogStripping = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean accurateBlockPlacement = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean enchantmentDescriptions = true;

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
        @ConfigEntry.Gui.Tooltip public boolean noCropTrampleFeatherFalling = true;
        @ConfigEntry.Gui.Tooltip public boolean noCropTrampleMobs = true;
    }

    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.CollapsibleObject
    public InstaMineConfig instaMine = new InstaMineConfig();
    public static class InstaMineConfig {
        @ConfigEntry.Gui.Tooltip public boolean instaMineGlass = true;
        @ConfigEntry.Gui.Tooltip public boolean instaMineDeepslateWithHaste = true;
    }

    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.CollapsibleObject
    public HappyGhastConfig happyGhast = new HappyGhastConfig();
    public static class HappyGhastConfig {
        @ConfigEntry.Gui.Tooltip public boolean speedHappyGhast = true;
        @ConfigEntry.Gui.Tooltip public boolean horizontalSpeedEnabled = true;
        @ConfigEntry.Gui.Tooltip public String happyGhastHorizontalBoost = "100";
        @ConfigEntry.Gui.Tooltip public boolean verticalSpeedEnabled = true;
        @ConfigEntry.Gui.Tooltip public String happyGhastVerticalBoost = "100";
    }

    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.CollapsibleObject
    public EssentialConfig essential = new EssentialConfig();
    public static class EssentialConfig {
        @ConfigEntry.Gui.Tooltip public boolean glowingTrims = true;
        @ConfigEntry.Gui.Tooltip public boolean coralReborn = true;
        @ConfigEntry.Gui.Tooltip public boolean moreDyes = true;
        @ConfigEntry.Gui.Tooltip public boolean hideBeaconBeamBehindTintedGlass = true;
        @ConfigEntry.Gui.Tooltip public boolean noBatSpawning = true;
        @ConfigEntry.Gui.Tooltip public boolean anvilNoTooExpensive = true;
        @ConfigEntry.Gui.Tooltip public boolean playerDropsNeverDespawn = true;
        @ConfigEntry.Gui.Tooltip public boolean infiniteVillagerTrading = true;
        @ConfigEntry.Gui.Tooltip public boolean enchantThroughFoliage = true;
        @ConfigEntry.Gui.Tooltip public boolean trueVanillaGamemodeLock = false;
    }

    // ========================== PERFORMANCE TAB ==========================
    @ConfigEntry.Category("performance") @ConfigEntry.Gui.Tooltip public boolean stackingXp = true;

    // ============================= FIXES TAB =============================
    @ConfigEntry.Category("fixes") @ConfigEntry.Gui.CollapsibleObject
    public McBugsConfig mcBugs = new McBugsConfig();
    public static class McBugsConfig {
        @ConfigEntry.Gui.Tooltip public boolean enderPearlFix = true;
        @ConfigEntry.Gui.Tooltip public boolean stackedMinecartFix = true;
        @ConfigEntry.Gui.Tooltip public boolean rocketElytraFix = true;
        @ConfigEntry.Gui.Tooltip public boolean netherPortalFix = true;
    }

    @ConfigEntry.Category("fixes") @ConfigEntry.Gui.CollapsibleObject
    public OtherFixesConfig otherFixes = new OtherFixesConfig();
    public static class OtherFixesConfig {
        @ConfigEntry.Gui.Tooltip public boolean piglinPortalFix = true;
        @ConfigEntry.Gui.Tooltip public boolean safeRedstone = true;
        @ConfigEntry.Gui.Tooltip public boolean noAnimalTemptDelay = true;
    }

    // ============= EXCLUDED MIRRORS =============
    @ConfigEntry.Category("general") @ConfigEntry.Gui.Excluded public transient GeneralSettingsConfig generalSettings = new GeneralSettingsConfig();
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Excluded public transient PlayerHudConfig hud = new PlayerHudConfig();
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Excluded public transient String zoomKey = "C", happyGhastBoost = "100", happyGhastHorizontalBoost = "100", happyGhastVerticalBoost = "100";
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Excluded public transient boolean modEnabled = true, kindaCheaty = false, clearWater = true, clearLava = true, clearPowderSnow = true, favouriteWorlds = true, worldPlaytimeDisplay = true, deletedWorldProtection = true, reachBehind = true, reachBehindSign = true, armorStandSwap = true, horseInfo = false, dismountOnClick = true, autoPickup = true, autoClimbLadders = true, autoTotem = true, closeCall = true, autoToolSwitch = true, simpleHarvest = true, stackRefill = true, betterF1 = true, disableNarrator = true, disableAdvancementKeybind = true, ctrlDropAllSameItems = true, bundleFix = true, moreChatHistory = true, hideArmor = true, centralCrosshair = true, raisedHotbar = true, chatTextCopy = true, headLocator = true, locatorBarCombination = true, recipeBookOrigin = true, chatHeads = true, hideOwnParticlesFirstPerson = true, hideOwnParticlesThirdPerson = true, bookScroll = true, zoom = true, dynamicLights = true, chestSearchBar = true, netherCalc = true, noResourcePackWarning = true, fastLanguageReload = true, fastQuit = true, fadelessLoading = true, borderlessFullscreen = true, safeLightning = true, noZombieDoorBreak = true, noCropTrampleFeatherFalling = true, noCropTrampleMobs = true, instaMineGlass = true, instaMineDeepslateWithHaste = true, speedHappyGhast = true, glowingTrims = true, coralReborn = true, hideBeaconBeamBehindTintedGlass = true, noBatSpawning = true, anvilNoTooExpensive = true, playerDropsNeverDespawn = true, infiniteVillagerTrading = true, enchantThroughFoliage = true, trueVanillaGamemodeLock = false, moreDyes = true, enderPearlFix = true, stackedMinecartFix = true, rocketElytraFix = true, netherPortalFix = true, piglinPortalFix = true, safeRedstone = true, noAnimalTemptDelay = true;

    @Override public void validatePostLoad() { syncMirrors(); }

    public void syncMirrors() {
        if (general != null) {
            modEnabled = general.modEnabled;
            kindaCheaty = general.kindaCheaty;
        }
        if (kindaCheaty && autoSettings != null) {
            autoSettings.autoTotem = false;
            autoSettings.closeCall = false;
        }
        if (clearVision != null) { clearWater = clearVision.clearWater; clearLava = clearVision.clearLava; clearPowderSnow = clearVision.clearPowderSnow; }
        if (worldSettings != null) { favouriteWorlds = worldSettings.favouriteWorlds; worldPlaytimeDisplay = worldSettings.worldPlaytimeDisplay; deletedWorldProtection = worldSettings.deletedWorldProtection; }
        if (entities != null) { reachBehind = entities.reachBehind; reachBehindSign = entities.reachBehindSign; armorStandSwap = entities.armorStandSwap; horseInfo = false; dismountOnClick = entities.dismountOnClick; }
        if (autoSettings != null) { autoPickup = autoSettings.autoPickup; autoClimbLadders = autoSettings.autoClimbLadders; autoTotem = !kindaCheaty && autoSettings.autoTotem; closeCall = !kindaCheaty && autoSettings.closeCall; autoToolSwitch = autoSettings.autoToolSwitch; simpleHarvest = autoSettings.simpleHarvest; }
        if (playerHud != null) { hud = playerHud; stackRefill = playerHud.stackRefill; betterF1 = playerHud.betterF1; bundleFix = playerHud.bundleFix; hideArmor = playerHud.hideArmor; raisedHotbar = playerHud.raisedHotbar; headLocator = playerHud.headLocator; locatorBarCombination = playerHud.locatorBarCombination; chatHeads = playerHud.chatHeads; hideOwnParticlesFirstPerson = playerHud.hideOwnParticlesFirstPerson; hideOwnParticlesThirdPerson = playerHud.hideOwnParticlesThirdPerson; centralCrosshair = playerHud.centralCrosshair; }
        if (remainingHuds != null) { disableNarrator = remainingHuds.disableNarrator; disableAdvancementKeybind = remainingHuds.disableAdvancementKeybind; ctrlDropAllSameItems = remainingHuds.ctrlDropAllSameItems; moreChatHistory = remainingHuds.moreChatHistory; chatTextCopy = remainingHuds.chatTextCopy; recipeBookOrigin = remainingHuds.recipeBookOrigin; bookScroll = remainingHuds.bookScroll; zoom = remainingHuds.zoom; zoomKey = remainingHuds.zoomKey; dynamicLights = remainingHuds.dynamicLights; chestSearchBar = remainingHuds.chestSearchBar; }
        if (commands != null) { netherCalc = commands.netherCalc; }
        if (screenSettings != null) { noResourcePackWarning = screenSettings.noResourcePackWarning; fastLanguageReload = screenSettings.fastLanguageReload; fastQuit = screenSettings.fastQuit; fadelessLoading = screenSettings.fadelessLoading; borderlessFullscreen = screenSettings.borderlessFullscreen; }
        if (mobGriefing != null) { safeLightning = mobGriefing.safeLightning; noZombieDoorBreak = mobGriefing.noZombieDoorBreak; noCropTrampleFeatherFalling = mobGriefing.noCropTrampleFeatherFalling; noCropTrampleMobs = mobGriefing.noCropTrampleMobs; }
        if (instaMine != null) { instaMineGlass = instaMine.instaMineGlass; instaMineDeepslateWithHaste = instaMine.instaMineDeepslateWithHaste; }
        if (happyGhast != null) { speedHappyGhast = happyGhast.speedHappyGhast; happyGhastHorizontalBoost = happyGhast.happyGhastHorizontalBoost; happyGhastVerticalBoost = happyGhast.happyGhastVerticalBoost; happyGhastBoost = happyGhast.happyGhastHorizontalBoost; }
        if (essential != null) { glowingTrims = essential.glowingTrims; coralReborn = essential.coralReborn; hideBeaconBeamBehindTintedGlass = essential.hideBeaconBeamBehindTintedGlass; noBatSpawning = essential.noBatSpawning; anvilNoTooExpensive = essential.anvilNoTooExpensive; playerDropsNeverDespawn = essential.playerDropsNeverDespawn; infiniteVillagerTrading = essential.infiniteVillagerTrading; enchantThroughFoliage = essential.enchantThroughFoliage; trueVanillaGamemodeLock = essential.trueVanillaGamemodeLock; moreDyes = essential.moreDyes; }
        if (mcBugs != null) { enderPearlFix = mcBugs.enderPearlFix; stackedMinecartFix = mcBugs.stackedMinecartFix; rocketElytraFix = mcBugs.rocketElytraFix; netherPortalFix = mcBugs.netherPortalFix; }
        if (otherFixes != null) { piglinPortalFix = otherFixes.piglinPortalFix; safeRedstone = otherFixes.safeRedstone; noAnimalTemptDelay = otherFixes.noAnimalTemptDelay; }
    }
}
// END OF FILE
