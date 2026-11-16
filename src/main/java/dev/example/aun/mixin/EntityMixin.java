package dev.example.aun.mixin;

import dev.example.aun.config.AunConfig;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({Entity.class, CraftingMenu.class, InventoryMenu.class})
public abstract class EntityMixin {

    @Inject(method = "slotsChanged", at = @At("TAIL"), require = 0)
    private void aun$applyCodeBasedMoreDyesRecipes(Container container, CallbackInfo ci) {
        if (container == null) return;
        AunConfig cfg = AunConfigManager.get();
        if (cfg.general != null && !cfg.general.modEnabled) return;
        if (cfg.essential != null && !cfg.essential.moreDyes) return;

        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (menu.slots.isEmpty()) return;

        Slot resultSlot = menu.slots.get(0);
        if (resultSlot.hasItem()) return;

        ItemStack singleIngredient = ItemStack.EMPTY;
        int nonEmptyCount = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                nonEmptyCount++;
                if (nonEmptyCount > 1) return;
                singleIngredient = stack;
            }
        }

        if (nonEmptyCount != 1 || singleIngredient.isEmpty()) return;

        ItemStack dyeOutput = aun$getMoreDyesOutput(singleIngredient);
        if (!dyeOutput.isEmpty()) {
            resultSlot.set(dyeOutput);
            menu.broadcastChanges();
        }
    }

    @Unique
    private static ItemStack aun$getMoreDyesOutput(ItemStack ingredient) {
        String path = BuiltInRegistries.ITEM.getKey(ingredient.getItem()).getPath();
        String dyeId = switch (path) {
            case "redstone", "fire_coral", "fire_coral_fan" -> "red_dye";
            case "coal", "charcoal" -> "black_dye";
            case "tube_coral", "tube_coral_fan" -> "blue_dye";
            case "brain_coral", "brain_coral_fan" -> "pink_dye";
            case "bubble_coral", "bubble_coral_fan" -> "purple_dye";
            case "horn_coral", "horn_coral_fan" -> "yellow_dye";
            default -> null;
        };
        if (dyeId == null) return ItemStack.EMPTY;
        Item dyeItem = BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(dyeId));
        if (dyeItem == null || dyeItem == Items.AIR) return ItemStack.EMPTY;
        return new ItemStack(dyeItem, 1);
    }
}
// END OF FILE
