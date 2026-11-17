package dev.example.aun.config;

import me.shedaniel.autoconfig.annotation.ConfigEntry;

public class AunConfigQol {

    public static class ClearVisionConfig {
        @ConfigEntry.Gui.Tooltip public boolean clearWater = true;
        @ConfigEntry.Gui.Tooltip public boolean clearLava = true;
        @ConfigEntry.Gui.Tooltip public boolean clearPowderSnow = true;
    }

    public static class WorldSettingsConfig {
        @ConfigEntry.Gui.Tooltip public boolean favouriteWorlds = true;
        @ConfigEntry.Gui.Tooltip public boolean worldPlaytimeDisplay = true;
        @ConfigEntry.Gui.Tooltip public boolean deletedWorldProtection = true;
    }

    public static class EntitiesConfig {
        @ConfigEntry.Gui.Tooltip public boolean reachBehind = true;
        @ConfigEntry.Gui.Tooltip public boolean reachBehindSign = true;
        @ConfigEntry.Gui.Tooltip public boolean armorStandSwap = true;
        @ConfigEntry.Gui.Tooltip public boolean horseInfo = true;
        @ConfigEntry.Gui.Tooltip public boolean dismountOnClick = true;
    }

    public static class AutoSettingsConfig {
        @ConfigEntry.Gui.Tooltip public boolean autoPickup = true;
        @ConfigEntry.Gui.Tooltip public boolean autoClimbLadders = true;
        @ConfigEntry.Gui.Tooltip public boolean autoTotem = true;
        @ConfigEntry.Gui.Tooltip public boolean closeCall = true;
        @ConfigEntry.Gui.Tooltip public boolean autoToolSwitch = true;
        @ConfigEntry.Gui.Tooltip public boolean simpleHarvest = true;
    }

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

    public static class CommandsConfig {
        @ConfigEntry.Gui.Tooltip public boolean netherCalc = true;
    }

    public static class ScreenConfig {
        @ConfigEntry.Gui.Tooltip public boolean noResourcePackWarning = true;
        @ConfigEntry.Gui.Tooltip public boolean fastLanguageReload = true;
        @ConfigEntry.Gui.Tooltip public boolean fastQuit = true;
        @ConfigEntry.Gui.Tooltip public boolean fadelessLoading = true;
        @ConfigEntry.Gui.Tooltip public boolean borderlessFullscreen = true;
    }

    public static class HideNametagsConfig {
        @ConfigEntry.Gui.Tooltip public boolean hidePlayerNametags = false;
        @ConfigEntry.Gui.Tooltip public boolean hideMobNametags = false;
        @ConfigEntry.Gui.Tooltip public boolean hideHostileMobNametags = false;
        @ConfigEntry.Gui.Tooltip public boolean hidePassiveMobNametags = false;
        @ConfigEntry.Gui.Tooltip public boolean hideTamedMobNametags = false;
        @ConfigEntry.Gui.Tooltip public String hiddenMobsList = "";
    }
}
// END OF FILE
