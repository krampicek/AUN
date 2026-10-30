package dev.example.tweakkit.config;

import dev.example.tweakkit.TweakKit;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

/**
 * Pure data holder for the config. Cloth Config's AutoConfig generates the GUI
 * from these public fields and serialises them to config/tweakkit.json.
 * Translation keys live in assets/tweakkit/lang/en_us.json.
 */
@Config(name = TweakKit.MOD_ID)
public class TweakConfig implements ConfigData {

    /** Feature 1: Endermen cannot pick up / place blocks. */
    @ConfigEntry.Gui.Tooltip
    public boolean antiEndermanPickup = true;

    /** Feature 2: tinted glass above a beacon hides the beam, effects keep working. */
    @ConfigEntry.Gui.Tooltip
    public boolean hideBeaconBeamBehindTintedGlass = true;

    /** Feature 3: lightning never spawns fire. */
    @ConfigEntry.Gui.Tooltip
    public boolean safeLightning = true;

    /** Feature 4: Netherite pickaxe + Efficiency V insta-mines glass. */
    @ConfigEntry.Gui.Tooltip
    public boolean instaMineGlass = true;

    /** Feature 5: Netherite pickaxe + Efficiency V + Haste II insta-mines deepslate. */
    @ConfigEntry.Gui.Tooltip
    public boolean instaMineDeepslateWithHaste = true;
}
