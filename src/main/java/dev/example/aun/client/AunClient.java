package dev.example.aun.client;

import net.fabricmc.api.ClientModInitializer;

/** Client-only entrypoint. */
public class AunClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        CutThroughHandler.register();
        AutoToolSwitch.register();
    }
}
