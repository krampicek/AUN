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

    // --------------------------- Vanilla+ ----------------------------
    @ConfigEntry.Category("vanillaPlus") @ConfigEntry.Gui.Tooltip public boolean antiEndermanPickup = true;
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

    // ------------------------------ Fixes ----------------------------
    @ConfigEntry.Category("fixes") @ConfigEntry.Gui.Tooltip public boolean piglinPortalFix = true;
    @ConfigEntry.Category("fixes") @ConfigEntry.Gui.Tooltip public boolean safeRedstone = true;
}
