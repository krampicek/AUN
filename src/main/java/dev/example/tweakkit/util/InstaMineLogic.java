package dev.example.tweakkit.util;

import dev.example.tweakkit.config.TweakConfig;
import dev.example.tweakkit.config.TweakConfigManager;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Decision logic for features 4 and 5 (Mojang names). */
public final class InstaMineLogic {

    private InstaMineLogic() {}

    public static boolean shouldInstaMine(BlockState state, Player player) {
        TweakConfig cfg = TweakConfigManager.get();
        if (!cfg.instaMineGlass && !cfg.instaMineDeepslateWithHaste) {
            return false;
        }
        if (!isNetheritePickaxeWithEfficiencyFive(player.getMainHandItem())) {
            return false;
        }
        if (cfg.instaMineGlass && isGlass(state)) {
            return true;
        }
        return cfg.instaMineDeepslateWithHaste && isDeepslate(state) && hasHasteTwoOrHigher(player);
    }

    private static boolean isNetheritePickaxeWithEfficiencyFive(ItemStack stack) {
        if (!stack.is(Items.NETHERITE_PICKAXE)) {
            return false;
        }
        ItemEnchantments enchants = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> entry : enchants.keySet()) {
            if (entry.is(Enchantments.EFFICIENCY) && enchants.getLevel(entry) >= 5) {
                return true;
            }
        }
        return false;
    }

    /** Beacon Haste II = amplifier 1. Any source of Haste II (or higher) counts. */
    private static boolean hasHasteTwoOrHigher(Player player) {
        MobEffectInstance haste = player.getEffect(MobEffects.HASTE);
        return haste != null && haste.getAmplifier() >= 1;
    }

    /** Glass, stained glass, tinted glass and panes (vanilla + convention-tagged modded glass). */
    private static boolean isGlass(BlockState state) {
        if (state.is(Blocks.TINTED_GLASS)
            || state.is(ConventionalBlockTags.GLASS_BLOCKS)
            || state.is(ConventionalBlockTags.GLASS_PANES)) {
            return true;
        }
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return id.getNamespace().equals("minecraft") && id.getPath().contains("glass");
    }

    /**
     * Deepslate and building variants (cobbled, polished, bricks, tiles, chiseled, stairs,
     * slabs, walls). Excludes ores, infested and reinforced deepslate - edit here to change.
     */
    private static boolean isDeepslate(BlockState state) {
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!id.getNamespace().equals("minecraft")) {
            return false;
        }
        String path = id.getPath();
        return path.contains("deepslate")
            && !path.contains("_ore")
            && !path.startsWith("infested_")
            && !path.startsWith("reinforced_");
    }
}
