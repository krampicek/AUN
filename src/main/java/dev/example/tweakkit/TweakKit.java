package dev.example.tweakkit;

import dev.example.tweakkit.config.TweakConfigManager;
import dev.example.tweakkit.gamerule.TweakGameRules;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entrypoint. Runs on both the client and the dedicated server.
 * All gameplay logic lives in the mixins, which read the live config on every call.
 */
public class TweakKit implements ModInitializer {
    public static final String MOD_ID = "tweakkit";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        TweakConfigManager.register();
        TweakGameRules.register();
        LOGGER.info("TweakKit initialised - config loaded from config/{}.json", MOD_ID);
    }
}
