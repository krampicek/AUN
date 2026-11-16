package dev.example.aun.client;

import dev.example.aun.config.AunConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class AunConfigScreen extends Screen {

    private final Screen parent;

    public AunConfigScreen(Screen parent) {
        super(Component.literal("A-U-N Settings"));
        this.parent = parent;
    }

    public static Screen create(Screen parent) {
        return AutoConfig.getConfigScreen(AunConfig.class, parent).get();
    }

    @Override
    protected void init() {
        Minecraft.getInstance().setScreenAndShow(create(this.parent));
    }
}
// END OF FILE
