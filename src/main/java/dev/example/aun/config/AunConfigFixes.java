package dev.example.aun.config;

import me.shedaniel.autoconfig.annotation.ConfigEntry;

public class AunConfigFixes {
    
    public static class McBugsConfig {
        @ConfigEntry.Gui.Tooltip public boolean enderPearlFix = true;
        @ConfigEntry.Gui.Tooltip public boolean stackedMinecartFix = true;
        @ConfigEntry.Gui.Tooltip public boolean rocketElytraFix = true;
        @ConfigEntry.Gui.Tooltip public boolean netherPortalFix = true;
    }

    public static class OtherFixesConfig {
        @ConfigEntry.Gui.Tooltip public boolean piglinPortalFix = true;
        @ConfigEntry.Gui.Tooltip public boolean safeRedstone = true;
        @ConfigEntry.Gui.Tooltip public boolean noAnimalTemptDelay = true;
    }
}
// END OF FILE
