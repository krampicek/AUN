package dev.example.aun;

import dev.example.aun.config.AunConfigManager;
import dev.example.aun.gamerule.AunGameRules;
import dev.example.aun.handler.EntityUseHandlers;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A-U-N main entrypoint (client and dedicated server). */
public class Aun implements ModInitializer {
    public static final String MOD_ID = "aun";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Id of the attribute modifier used by the Happy Ghast speed feature. */
    public static final Identifier GHAST_SPEED_ID = Identifier.fromNamespaceAndPath(MOD_ID, "happy_ghast_speed");

    @Override
    public void onInitialize() {
        AunConfigManager.register();
        AunGameRules.register();
        EntityUseHandlers.register();
        LOGGER.info("A-U-N initialised - config in config/{}.json", MOD_ID);
    }
}
