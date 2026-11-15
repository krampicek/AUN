package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Book scroll: scroll the mouse wheel to turn the pages of a book you are reading.
 * The book screen has no mouse wheel handling of its own, so we add it.
 */
@Mixin(BookViewScreen.class)
public abstract class BookViewScreenMixin {

    @Shadow protected abstract void pageBack();

    @Shadow protected abstract void pageForward();

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!AunConfigManager.get().bookScroll || scrollY == 0.0) {
            return false;
        }
        if (scrollY < 0.0) {
            this.pageForward();
        } else {
            this.pageBack();
        }
        return true;
    }
}
