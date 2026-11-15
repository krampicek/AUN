package dev.example.aun.mixin;

import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @ModifyConstant(method = "*", constant = @Constant(intValue = 100), require = 0)
    private int aun$expandChatHistoryLimit(int original) {
        return AunConfigManager.get().moreChatHistory ? 1000 : original;
    }
}
