package dev.example.aun.config;

import dev.example.aun.Aun;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * One ON/OFF switch per feature, sorted into three tabs:
 *   qol          = "QOL"
 *   vanillaPlus  = "Vanilla+"
 *   fixes        = "Fixes"
 * Tab names and option labels live in assets/aun/lang/en_us.json.
 */
@Config(name = Aun.MOD_ID)
public class AunConfig implements ConfigData {

    // ------------------------------ QOL ------------------------------
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean reachBehind = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean reachBehindSign = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean cutThrough = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean dismountOnClick = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean armorStandSwap = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean doubleDoors = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean simpleHarvest = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean autoPickup = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean fastLeafDecay = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean horseInfo = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean closeCall = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean autoToolSwitch = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean bookScroll = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean fullbright = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean netherCalc = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean stackRefill = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean autoTotem = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean autoClimbLadders = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean noAccidentalLogStripping = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean zoom = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean betterF1 = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean disableNarrator = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean disableAdvancementKeybind = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean facingPlacement = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean accurateBlockPlacement = true;
    @ConfigEntry.Category("qol") @ConfigEntry.Gui.Tooltip public boolean ctrlDropAllSameItems = true;

    // --------------------------- Vanilla+ ----------------------------
    @ConfigEntry.Category("vanillaPlus")
    @ConfigEntry.Gui.CollapsibleObject
    public MobGriefingConfig mobGriefing = new MobGriefingConfig();

    public static class MobGriefingConfig {
        @ConfigEntry.Gui.Tooltip public boolean antiEndermanPickup = true;
        @ConfigEntry.Gui.Tooltip public boolean antiCreeperGrief = true;
        @ConfigEntry.Gui.Tooltip public boolean antiGhastGrief = true;
        @ConfigEntry.Gui.Tooltip public boolean antiBlazeGrief = true;
        @ConfigEntry.Gui.Tooltip public boolean antiRavagerGrief = true;
    }

    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean hideBeaconBeamBehindTintedGlass = true;
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean safeLightning = true;
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean instaMineGlass = true;
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean instaMineDeepslateWithHaste = true;
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean speedHappyGhast = true;
    /** Strength of the Happy Ghast boost in percent, typed as text. 100 = default, 200 = double, 0 = none. */
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public String happyGhastBoost = "100";
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean noBatSpawning = true;
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean anvilNoTooExpensive = true;
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean playerDropsNeverDespawn = true;
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean stackingXp = true;
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean infiniteVillagerTrading = true;
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean noZombieDoorBreak = true;
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean enchantThroughFoliage = true;
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean noCropTrampleFeatherFalling = true;
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean noCropTrampleMobs = true;
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean trueVanillaGamemodeLock = false;

    // ------------------------------ Fixes ----------------------------
    @ConfigEntry.Category("fixes") @ConfigEntry.Gui.Tooltip public boolean piglinPortalFix = true;
    @ConfigEntry.Category("fixes") @ConfigEntry.Gui.Tooltip public boolean safeRedstone = true;
    @ConfigEntry.Category("fixes") @ConfigEntry.Gui.Tooltip public boolean enderPearlFix = true;
    @ConfigEntry.Category("fixes") @ConfigEntry.Gui.Tooltip public boolean noAnimalTemptDelay = true;
    @ConfigEntry.Category("fixes") @ConfigEntry.Gui.Tooltip public boolean rocketElytraFix = true;
    @ConfigEntry.Category("fixes") @ConfigEntry.Gui.Tooltip public boolean stackedMinecartFix = true;
}
