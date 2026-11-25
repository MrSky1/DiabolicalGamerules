package com.diabolical.defaultgamerules;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class OldWorldSyncScreen extends Screen {
    private final boolean realPrompt;
    private final boolean testPrompt;
    private Checkbox dontAskAgain;

    public OldWorldSyncScreen(boolean realPrompt, boolean testPrompt) {
        super(Component.translatable("screen.diabolical_default_gamerules.title"));
        this.realPrompt = realPrompt;
        this.testPrompt = testPrompt;
    }

    @Override
    protected void init() {
        int center = width / 2;
        int buttonWidth = 100;
        int gap = 12;
        addRenderableWidget(Button.builder(Component.translatable("screen.diabolical_default_gamerules.yes"), button -> {
            NetworkHandler.respond(true, false, realPrompt, testPrompt);
            onClose();
        }).bounds(center - buttonWidth - gap / 2, height / 2 + 24, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("screen.diabolical_default_gamerules.no"), button -> {
            NetworkHandler.respond(false, dontAskAgain.selected(), realPrompt, testPrompt);
            onClose();
        }).bounds(center + gap / 2, height / 2 + 24, buttonWidth, 20).build());
        dontAskAgain = Checkbox.builder(Component.translatable("screen.diabolical_default_gamerules.dont_ask_again"), font).pos(center - 100, height / 2 + 54).selected(false).build();
        addRenderableWidget(dontAskAgain);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 54, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.diabolical_default_gamerules.description"), width / 2, height / 2 - 24, 0xFFFFFF);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public boolean isPauseScreen() { return true; }
}