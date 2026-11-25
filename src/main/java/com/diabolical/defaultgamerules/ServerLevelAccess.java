package com.diabolical.defaultgamerules;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

final class ServerLevelAccess {
    private ServerLevelAccess() {}

    static void handle(ServerPlayer player, NetworkHandler.PromptResponse response) {
        // Debug prompts are visual-only: never apply gamerules or change saved prompt state.
        if (!response.realPrompt()) return;

        DefaultGameruleWorldData data = DefaultGameruleWorldData.get(player.getServer().overworld());
        if (data.isSynced()) return;
        if (response.yes()) {
            int changed = GameruleDefaults.apply((ServerLevel) player.level());
            data.setSynced(true);
            player.sendSystemMessage(Component.translatable("message.diabolical_default_gamerules.sync_success", changed).withStyle(ChatFormatting.GREEN));
        } else if (response.dontAskAgain()) {
            data.setSynced(true);
            data.setPromptMessageShown(true);
        } else if (!data.isPromptMessageShown()) {
            player.sendSystemMessage(Component.translatable("message.diabolical_default_gamerules.prompt_again").withStyle(ChatFormatting.GOLD));
            data.setPromptMessageShown(true);
        }
    }
}