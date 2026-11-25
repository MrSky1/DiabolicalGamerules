package com.diabolical.defaultgamerules;

import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@Mod(DiabolicalDefaultGamerules.MOD_ID)
public class DiabolicalDefaultGamerules {
    public static final String MOD_ID = "diabolical_default_gamerules";

    public DiabolicalDefaultGamerules(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(this::onCreateSpawnPosition);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedIn);
        modEventBus.addListener(NetworkHandler::register);
        GameruleDefaults.initialize();
    }

    private void onCreateSpawnPosition(LevelEvent.CreateSpawnPosition event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!level.dimension().equals(Level.OVERWORLD)) return;
        GameruleDefaults.apply(level);
        DefaultGameruleWorldData.get(level).setSynced(true);
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) || !GameruleDefaults.shouldPromptOldWorlds()) return;
        DefaultGameruleWorldData data = DefaultGameruleWorldData.get(player.getServer().overworld());
        if (!data.isSynced()) NetworkHandler.openPrompt(player, true, false);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("diabolical_gamerules")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("save_defaults")
                                .executes(context -> {
                                    int count = GameruleDefaults.saveDefaults(context.getSource().getServer());
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Saved " + count + " gamerule changes to the config."),
                                            true
                                    );
                                    return count;
                                }))
                        .then(Commands.literal("sync_gamerules")
                                .executes(context -> {
                                    int count = GameruleDefaults.apply(context.getSource().getLevel());
                                    if (count == 0) {
                                        context.getSource().sendFailure(Component.literal(
                                                "No gamerule changes applied. Check that the config contains gamerules that differ from the defaults."
                                        ));
                                        return 0;
                                    }
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Applied " + count + " gamerule changes to this world."),
                                            true
                                    );
                                    return count;
                                }))
                        .then(Commands.literal("test_prompt")
                                .then(Commands.argument("real_prompt", BoolArgumentType.bool())
                                        .executes(context -> {
                                            boolean realPrompt = BoolArgumentType.getBool(context, "real_prompt");
                                            NetworkHandler.openPrompt(context.getSource().getPlayerOrException(), realPrompt, true);
                                            context.getSource().sendSuccess(
                                                    () -> Component.literal(realPrompt
                                                            ? "Opened a real sync prompt."
                                                            : "Opened a debug prompt. Responses won't change anything."),
                                                    false
                                            );
                                            return 1;
                                        })))
        );
    }
}
