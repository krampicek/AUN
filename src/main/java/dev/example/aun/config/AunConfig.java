package dev.example.aun.config;

import dev.example.aun.Aun;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;

import java.util.Locale;

@Config(name = Aun.MOD_ID)
public class AunConfig implements ConfigData {

    public enum ProfileMode { DEFAULT, ALL_ON, ALL_OFF, OWN_PROFILE }
    public enum LinkAction { LINK, OPEN_LINK; @Override public String toString() { return "LINK"; } }
    public enum ExportAction { EXPORT, OPEN_EXPORT; @Override public String toString() { return "EXPORT"; } }
    public enum LanguageAction { CHOOSE_LANGUAGE, OPEN_LANGUAGE; @Override public String toString() { return "CHOOSE_LANGUAGE"; } }

    // ============================== GENERAL TAB ==============================
    @ConfigEntry.Category("general") @ConfigEntry.Gui.TransitiveObject
    public AunConfigGeneral.GeneralSettingsConfig general = new AunConfigGeneral.GeneralSettingsConfig();

    @ConfigEntry.Category("general") @ConfigEntry.Gui.CollapsibleObject
    public AunConfigGeneral.ProfileSubConfig profile = new AunConfigGeneral.ProfileSubConfig();

    @ConfigEntry.Category("general") @ConfigEntry.Gui.CollapsibleObject
    public AunConfigGeneral.LanguageSubConfig languageSub = new AunConfigGeneral.LanguageSubConfig();

    @ConfigEntry.Category("general") @ConfigEntry.Gui.Excluded
    public String selectedLanguage = "en_us";

    // ============================== QOL TAB ==============================
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.CollapsibleObject
    public AunConfigQol.ClearVisionConfig clearVision = new AunConfigQol.ClearVisionConfig();

    @ConfigEntry.Category("qol") @ConfigEntry.Gui.CollapsibleObject
    public AunConfigQol.WorldSettingsConfig worldSettings = new AunConfigQol.WorldSettingsConfig();

    @ConfigEntry.Category("qol") @ConfigEntry.Gui.CollapsibleObject
    public AunConfigQol.EntitiesConfig entities = new AunConfigQol.EntitiesConfig();

    @ConfigEntry.Category("qol") @ConfigEntry.Gui.CollapsibleObject
    public AunConfigQol.AutoSettingsConfig autoSettings = new AunConfigQol.AutoSettingsConfig();

    @ConfigEntry.Category("qol") @ConfigEntry.Gui.CollapsibleObject
    public AunConfigQol.PlayerHudConfig playerHud = new AunConfigQol.PlayerHudConfig();

    @ConfigEntry.Category("qol") @ConfigEntry.Gui.CollapsibleObject
    public AunConfigQol.RemainingHudsConfig remainingHuds = new AunConfigQol.RemainingHudsConfig();

    @ConfigEntry.Category("qol") @ConfigEntry.Gui.CollapsibleObject
    public AunConfigQol.CommandsConfig commands = new AunConfigQol.CommandsConfig();

    @ConfigEntry.Category("qol") @ConfigEntry.Gui.CollapsibleObject
    public AunConfigQol.ScreenConfig screenSettings = new AunConfigQol.ScreenConfig();

    @ConfigEntry.Category("qol") @ConfigEntry.Gui.CollapsibleObject
    public AunConfigQol.HideNametagsConfig hideNametags = new AunConfigQol.HideNametagsConfig();

    // Flat QOL toggles
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
    public AunConfigVanillaPlus.MobGriefingConfig mobGriefing = new AunConfigVanillaPlus.MobGriefingConfig();

    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.CollapsibleObject
    public AunConfigVanillaPlus.InstaMineConfig instaMine = new AunConfigVanillaPlus.InstaMineConfig();

    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.CollapsibleObject
    public AunConfigVanillaPlus.HappyGhastConfig happyGhast = new AunConfigVanillaPlus.HappyGhastConfig();

    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.CollapsibleObject
    public AunConfigVanillaPlus.EssentialConfig essential = new AunConfigVanillaPlus.EssentialConfig();

    // ========================== PERFORMANCE TAB ==========================
    @ConfigEntry.Category("performance") @ConfigEntry.Gui.Tooltip public boolean stackingXp = true;

    // ============================= FIXES TAB =============================
    @ConfigEntry.Category("fixes") @ConfigEntry.Gui.CollapsibleObject
    public AunConfigFixes.McBugsConfig mcBugs = new AunConfigFixes.McBugsConfig();

    @ConfigEntry.Category("fixes") @ConfigEntry.Gui.CollapsibleObject
    public AunConfigFixes.OtherFixesConfig otherFixes = new AunConfigFixes.OtherFixesConfig();

    // ============= EXCLUDED MIRRORS =============
    @ConfigEntry.Category("general") @ConfigEntry.Gui.Excluded public transient AunConfigGeneral.GeneralSettingsConfig generalSettings = new AunConfigGeneral.GeneralSettingsConfig();
    @ConfigEntry.Category("general") @ConfigEntry.Gui.Excluded public transient AunConfigGeneral.LanguageSubConfig languageSubConfig = new AunConfigGeneral.LanguageSubConfig();
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Excluded public transient AunConfigQol.PlayerHudConfig hud = new AunConfigQol.PlayerHudConfig();
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Excluded public transient String zoomKey = "C", happyGhastBoost = "100", happyGhastHorizontalBoost = "100", happyGhastVerticalBoost = "100";
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Excluded public transient boolean modEnabled = true, kindaCheaty = false, clearWater = true, clearLava = true, clearPowderSnow = true, favouriteWorlds = true, worldPlaytimeDisplay = true, deletedWorldProtection = true, reachBehind = true, reachBehindSign = true, armorStandSwap = true, horseInfo = false, dismountOnClick = true, autoPickup = true, autoClimbLadders = true, autoTotem = true, closeCall = true, autoToolSwitch = true, simpleHarvest = true, stackRefill = true, betterF1 = true, disableNarrator = true, disableAdvancementKeybind = true, ctrlDropAllSameItems = true, bundleFix = true, moreChatHistory = true, hideArmor = true, centralCrosshair = true, raisedHotbar = true, chatTextCopy = true, headLocator = true, locatorBarCombination = true, recipeBookOrigin = true, chatHeads = true, hideOwnParticlesFirstPerson = true, hideOwnParticlesThirdPerson = true, bookScroll = true, zoom = true, dynamicLights = true, chestSearchBar = true, netherCalc = true, noResourcePackWarning = true, fastLanguageReload = true, fastQuit = true, fadelessLoading = true, borderlessFullscreen = true, safeLightning = true, noZombieDoorBreak = true, noCropTrampleFeatherFalling = true, noCropTrampleMobs = true, instaMineGlass = true, instaMineDeepslateWithHaste = true, speedHappyGhast = true, glowingTrims = true, coralReborn = true, hideBeaconBeamBehindTintedGlass = true, noBatSpawning = true, anvilNoTooExpensive = true, playerDropsNeverDespawn = true, infiniteVillagerTrading = true, enchantThroughFoliage = true, trueVanillaGamemodeLock = false, moreDyes = true, enderPearlFix = true, stackedMinecartFix = true, rocketElytraFix = true, netherPortalFix = true, piglinPortalFix = true, safeRedstone = true, noAnimalTemptDelay = true;

    @Override public void validatePostLoad() { syncMirrors(); }

    public void syncMirrors() {
        if (general != null) {
            modEnabled = general.modEnabled;
            kindaCheaty = general.kindaCheaty;
        }
        if (profile != null) {
            profile.linkProfile = LinkAction.LINK;
            profile.exportOwnProfile = ExportAction.EXPORT;
        }
        if (languageSub != null) {
            languageSub.chooseLanguage = LanguageAction.CHOOSE_LANGUAGE;
            if (languageSub.syncWithMinecraft && FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                try {
                    String mcLang = Minecraft.getInstance().getLanguageManager().getSelected();
                    if (mcLang != null && !mcLang.isBlank()) {
                        selectedLanguage = mcLang.toLowerCase(Locale.ROOT).replace('-', '_');
                    }
                } catch (Throwable ignored) {}
            }
        }
        if (kindaCheaty) {
            autoTotem = false;
            closeCall = false;
            if (autoSettings != null) {
                autoSettings.autoTotem = false;
                autoSettings.closeCall = false;
            }
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
