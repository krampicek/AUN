package dev.example.aun.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.example.aun.config.AunConfig;
import me.shedaniel.autoconfig.AutoConfigClient;

/** Adds the "Configure" button in Mod Menu. */
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> AutoConfigClient.getConfigScreen(AunConfig.class, parent).get();
    }
}
