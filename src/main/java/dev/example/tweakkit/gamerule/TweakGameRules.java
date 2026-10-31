package dev.example.tweakkit.gamerule;

import dev.example.tweakkit.TweakKit;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;

/**
 * Custom game rules (not shown in the TweakKit menu on purpose).
 * Change them in-game with:
 *   /gamerule tweakkit:macenerfdmgplayer true
 *   /gamerule tweakkit:maceplayerthorns true
 *   /gamerule tweakkit:maceplayerthornslevel 150
 * (Tab-complete works: type "/gamerule mace" and press Tab.)
 */
public final class TweakGameRules {

    /** Mace smash hit from a player on a player always deals exactly 1 HP (half a heart). */
    public static final GameRule<Boolean> MACE_NERF_DAMAGE_PLAYER = GameRuleBuilder
        .forBoolean(false)
        .category(GameRuleCategory.MISC)
        .buildAndRegister(Identifier.fromNamespaceAndPath(TweakKit.MOD_ID, "macenerfdmgplayer"));

    /** Boost the victim's Thorns when a player smashes another player with a mace. */
    public static final GameRule<Boolean> MACE_PLAYER_THORNS = GameRuleBuilder
        .forBoolean(false)
        .category(GameRuleCategory.MISC)
        .buildAndRegister(Identifier.fromNamespaceAndPath(TweakKit.MOD_ID, "maceplayerthorns"));

    /** Extra thorns damage in percent. 0 = vanilla, 100 = double, 150 = 2.5x. */
    public static final GameRule<Integer> MACE_PLAYER_THORNS_LEVEL = GameRuleBuilder
        .forInteger(100)
        .category(GameRuleCategory.MISC)
        .buildAndRegister(Identifier.fromNamespaceAndPath(TweakKit.MOD_ID, "maceplayerthornslevel"));

    private TweakGameRules() {}

    /** Called from the mod initializer so the rules above get registered at startup. */
    public static void register() {
        // Touching this class runs the static initialisers above.
    }
}
