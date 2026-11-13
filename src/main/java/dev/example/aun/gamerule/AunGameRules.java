package dev.example.aun.gamerule;

import dev.example.aun.Aun;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

/**
 * Game rule (not in the menu):  /gamerule aun:macenerfdmgplayer true
 * Tab-complete works: type "/gamerule mace" and press Tab.
 */
public final class AunGameRules {

    /** A mace smash from a player on a player always deals exactly 1 HP (half a heart). */
    public static final GameRule<Boolean> MACE_NERF_DAMAGE_PLAYER = GameRuleBuilder
        .forBoolean(false)
        .category(GameRuleCategory.MISC)
        .buildAndRegister(Identifier.fromNamespaceAndPath(Aun.MOD_ID, "macenerfdmgplayer"));

    private AunGameRules() {}

    public static void register() {
        // Touching this class runs the static initialiser above.
    }
}
