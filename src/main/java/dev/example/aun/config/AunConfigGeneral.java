package dev.example.aun.config;

import me.shedaniel.autoconfig.annotation.ConfigEntry;

public class AunConfigGeneral {

    public enum SaveAction {
        SAVE, CLICKED;
        @Override public String toString() { return "Save (Keep Open)"; }
    }

    public enum BrowseAction {
        BROWSE, CLICKED;
        @Override public String toString() { return "Browse Profiles"; }
    }

    public static class GeneralSettingsConfig {
        @ConfigEntry.Gui.Tooltip public boolean modEnabled = true;
        @ConfigEntry.Gui.Tooltip public String toggleKey = "N";
        @ConfigEntry.Gui.Tooltip public boolean kindaCheaty = false;
        
        @ConfigEntry.Gui.Tooltip 
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public SaveAction saveConfig = SaveAction.SAVE;

        @ConfigEntry.Gui.Tooltip 
        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public BrowseAction browseFolder = BrowseAction.BROWSE;
    }

    public static class ProfileSubConfig {
        @ConfigEntry.Gui.Tooltip @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public AunConfig.ProfileMode profileMode = AunConfig.ProfileMode.DEFAULT;
        @ConfigEntry.Gui.Tooltip @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public AunConfig.LinkAction linkProfile = AunConfig.LinkAction.LINK;
        @ConfigEntry.Gui.Tooltip @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public AunConfig.ExportAction exportOwnProfile = AunConfig.ExportAction.EXPORT;
        @ConfigEntry.Gui.Excluded public String customProfilePath = "default_profile.json";
    }

    public static class LanguageSubConfig {
        @ConfigEntry.Gui.Tooltip public boolean syncWithMinecraft = true;
        @ConfigEntry.Gui.Tooltip @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public AunConfig.LanguageAction chooseLanguage = AunConfig.LanguageAction.CHOOSE_LANGUAGE;
    }
}
// END OF FILE
