package dev.example.aun.mixin;

import dev.example.aun.client.AunWorldFavorites;
import dev.example.aun.config.AunConfigManager;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @ModifyConstant(method = "*", constant = @Constant(intValue = 100), require = 0)
    private int aun$expandChatHistoryLimit(int original) {
        return AunConfigManager.get().moreChatHistory ? 1000 : original;
    }

    @ModifyVariable(method = "addMessage*", at = @At("HEAD"), argsOnly = true, require = 0)
    private Component aun$decorateAndRecordChatMessage(Component message) {
        if (message == null) {
            return null;
        }
        AunWorldFavorites.recordChatMessage(message.getString());
        if (AunConfigManager.get().chatHeads) {
            return AunWorldFavorites.prependChatHeadBadge(message);
        }
        return message;
    }
}
