package dev.example.aun.config;

import me.shedaniel.autoconfig.annotation.ConfigEntry;

public class AunConfigVanillaPlus {

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

    public static class InstaMineConfig {
        @ConfigEntry.Gui.Tooltip public boolean instaMineGlass = true;
        @ConfigEntry.Gui.Tooltip public boolean instaMineDeepslateWithHaste = true;
    }

    public static class HappyGhastConfig {
        @ConfigEntry.Gui.Tooltip public boolean speedHappyGhast = true;
        @ConfigEntry.Gui.Tooltip public boolean horizontalSpeedEnabled = true;
        @ConfigEntry.Gui.Tooltip public String happyGhastHorizontalBoost = "100";
        @ConfigEntry.Gui.Tooltip public boolean verticalSpeedEnabled = true;
        @ConfigEntry.Gui.Tooltip public String happyGhastVerticalBoost = "100";
    }

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
}
// END OF FILE
