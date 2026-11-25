package com.diabolical.defaultgamerules;

import net.minecraft.client.Minecraft;

public final class ClientPromptHandler {
    private ClientPromptHandler() {}

    public static void open(boolean realPrompt) {
        Minecraft.getInstance().setScreen(new OldWorldSyncScreen(realPrompt));
    }
}