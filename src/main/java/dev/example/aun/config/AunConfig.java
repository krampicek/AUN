package dev.example.aun.config;

import dev.example.aun.Aun;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/** Data holder: one ON/OFF switch per feature. Labels live in assets/aun/lang/en_us.json. */
@Config(name = Aun.MOD_ID)
public class AunConfig implements ConfigData {

    @ConfigEntry.Gui.Tooltip public boolean antiEndermanPickup = true;
    @ConfigEntry.Gui.Tooltip public boolean hideBeaconBeamBehindTintedGlass = true;
    @ConfigEntry.Gui.Tooltip public boolean safeLightning = true;
    @ConfigEntry.Gui.Tooltip public boolean instaMineGlass = true;
    @ConfigEntry.Gui.Tooltip public boolean instaMineDeepslateWithHaste = true;

    @ConfigEntry.Gui.Tooltip public boolean reachBehind = true;
    @ConfigEntry.Gui.Tooltip public boolean cutThrough = true;
    @ConfigEntry.Gui.Tooltip public boolean dismountOnClick = true;
    @ConfigEntry.Gui.Tooltip public boolean speedHappyGhast = true;
    /** Strength of the Happy Ghast boost in percent. 100 = default, 200 = double, 0 = none. */
    @ConfigEntry.Gui.Tooltip public int happyGhastSpeedPercent = 100;
    @ConfigEntry.Gui.Tooltip public boolean armorStandSwap = true;
}
