package dev.example.tweakkit.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.example.tweakkit.config.TweakConfig;
import me.shedaniel.autoconfig.AutoConfigClient;

/** Adds the "Configure" button for this mod in Mod Menu. */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfigClient.getConfigScreen(TweakConfig.class, parent).get();
    }
}
