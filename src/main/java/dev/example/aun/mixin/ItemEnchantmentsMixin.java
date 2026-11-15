package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(ItemEnchantments.class)
public abstract class ItemEnchantmentsMixin {

    @Unique
    private static final Map<String, String> AUN_ENCHANT_DESCRIPTIONS = Map.ofEntries(
        Map.entry("protection", "Reduces most types of damage."),
        Map.entry("fire protection", "Reduces fire damage and burn time."),
        Map.entry("feather falling", "Reduces fall and ender pearl damage."),
        Map.entry("blast protection", "Reduces explosion damage and knockback."),
        Map.entry("projectile protection", "Reduces damage from arrows and projectiles."),
        Map.entry("respiration", "Extends underwater breathing time."),
        Map.entry("aqua affinity", "Increases underwater mining speed."),
        Map.entry("thorns", "Damages attackers when they hit you."),
        Map.entry("depth strider", "Increases underwater movement speed."),
        Map.entry("frost walker", "Turns water beneath your feet into frosted ice."),
        Map.entry("soul speed", "Increases movement speed on soul sand and soul soil."),
        Map.entry("swift sneak", "Increases movement speed while sneaking."),
        Map.entry("sharpness", "Increases melee damage."),
        Map.entry("smite", "Increases damage against undead mobs."),
        Map.entry("bane of arthropods", "Increases damage against spiders and arthropods."),
        Map.entry("knockback", "Increases melee knockback distance."),
        Map.entry("fire aspect", "Sets targets on fire when hit."),
        Map.entry("looting", "Mobs drop more items when killed."),
        Map.entry("sweeping edge", "Increases sweep attack damage."),
        Map.entry("efficiency", "Increases mining speed."),
        Map.entry("silk touch", "Mined blocks drop themselves instead of resources."),
        Map.entry("unbreaking", "Increases item durability."),
        Map.entry("fortune", "Increases block drops from ores and crops."),
        Map.entry("power", "Increases arrow damage."),
        Map.entry("punch", "Increases arrow knockback."),
        Map.entry("flame", "Arrows set targets on fire."),
        Map.entry("infinity", "Shooting consumes no regular arrows."),
        Map.entry("luck of the sea", "Increases treasure catch rate while fishing."),
        Map.entry("lure", "Decreases wait time when fishing."),
        Map.entry("loyalty", "Trident returns after being thrown."),
        Map.entry("impaling", "Increases trident damage to aquatic mobs."),
        Map.entry("riptide", "Launches you with the trident in water or rain."),
        Map.entry("channeling", "Summons lightning on hit during thunderstorms."),
        Map.entry("multishot", "Fires 3 arrows at the cost of 1."),
        Map.entry("quick charge", "Decreases crossbow reloading time."),
        Map.entry("piercing", "Arrows pass through multiple entities."),
        Map.entry("density", "Increases mace smash damage per block fallen."),
        Map.entry("breach", "Reduces target armor effectiveness against mace hits."),
        Map.entry("wind burst", "Launches you upward after a mace smash attack."),
        Map.entry("mending", "Repairs item durability using collected XP."),
        Map.entry("curse of binding", "Prevents removing the equipped item."),
        Map.entry("curse of vanishing", "Item disappears on death.")
    );

    @ModifyVariable(method = "addToTooltip", at = @At("HEAD"), argsOnly = true, require = 0)
    private Consumer<Component> aun$appendEnchantmentDescriptions(Consumer<Component> original) {
        if (!AunConfigManager.get().enchantmentDescriptions) {
            return original;
        }
        return line -> {
            original.accept(line);
            String plain = line.getString().toLowerCase(Locale.ROOT).trim();
            for (Map.Entry<String, String> entry : AUN_ENCHANT_DESCRIPTIONS.entrySet()) {
                if (plain.startsWith(entry.getKey())) {
                    original.accept(Component.literal("  §8- " + entry.getValue()));
                    break;
                }
            }
        };
    }
}
