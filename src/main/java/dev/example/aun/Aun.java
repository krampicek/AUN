package dev.example.aun;

import dev.example.aun.config.AunConfigManager;
import dev.example.aun.gamerule.AunGameRules;
import dev.example.aun.handler.AutoPickup;
import dev.example.aun.handler.BlockUseHandlers;
import dev.example.aun.handler.CloseCallHandler;
import dev.example.aun.handler.DropHandlers;
import dev.example.aun.handler.EntityUseHandlers;
import dev.example.aun.handler.LeafDecay;
import dev.example.aun.handler.SpawnHandlers;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A-U-N main entrypoint (client and dedicated server). Author: krampi_ */
public class Aun implements ModInitializer {
    public static final String MOD_ID = "aun";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Id of the attribute modifier used by the Happy Ghast speed feature. */
    public static final Identifier GHAST_SPEED_ID = Identifier.fromNamespaceAndPath(MOD_ID, "happy_ghast_speed");

    /** Scoreboard tag put on every item a player drops. */
    public static final String DROP_TAG = "aun_player_drop";
    private static volatile String session = "none";

    /** Tag that says "dropped during the current world session". */
    public static String sessionTag() {
        return "aun_s_" + session;
    }

    /** Called every time a world (server) starts, so leaving and re-entering a world is a new session. */
    public static void newSession() {
        session = Long.toString(System.nanoTime(), 36);
    }

    @Override
    public void onInitialize() {
        AunConfigManager.register();
        AunGameRules.register();
        EntityUseHandlers.register();
        BlockUseHandlers.register();
        AutoPickup.register();
        LeafDecay.register();
        SpawnHandlers.register();
        DropHandlers.register();
        CloseCallHandler.register();
        LOGGER.info("A-U-N initialised - config in config/{}.json", MOD_ID);
    }
}
