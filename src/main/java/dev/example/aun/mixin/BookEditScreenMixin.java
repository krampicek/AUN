package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import org.spongepowered.asm.mixin.Mixin;

import java.lang.reflect.Method;

/**
 * Book scroll for Book and Quill (BookEditScreen).
 */
@Mixin(BookEditScreen.class)
public abstract class BookEditScreenMixin {

    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!AunConfigManager.get().bookScroll || scrollY == 0.0) {
            return false;
        }
        String targetMethod = scrollY < 0.0 ? "pageForward" : "pageBack";
        try {
            Method method = BookEditScreen.class.getDeclaredMethod(targetMethod);
            method.setAccessible(true);
            method.invoke(this);
            return true;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
