package com.diabolical.defaultgamerules;

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
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        // Only apply defaults to a newly initialized Overworld, not existing worlds.
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return;
        }

        if (event.getSettings().isInitialized()) {
            return;
        }

        GameruleDefaults.apply(level);
        DefaultGameruleWorldData.get(level).setSynced(true);
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) || !GameruleDefaults.shouldPromptOldWorlds()) return;
        DefaultGameruleWorldData data = DefaultGameruleWorldData.get(player.getServer().overworld());
        if (!data.isSynced()) NetworkHandler.openPrompt(player);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("diabolical_gamerules")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("save_defaults")
                                .executes(context -> {
                                    int count = GameruleDefaults.saveDefaults(
                                            context.getSource().getServer()
                                    );

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "Saved " + count
                                                            + " gamerule defaults to the config."
                                            ),
                                            true
                                    );
                                    return count;
                                }))
                        .then(Commands.literal("sync_default_gamerules")
                                .executes(context -> {
                                    int count = GameruleDefaults.apply(
                                            context.getSource().getLevel()
                                    );

                                    if (count == 0) {
                                        context.getSource().sendFailure(
                                                Component.literal(
                                                        "No gamerules needed changing. Check that the config contains valid gamerule values that differ from this world's current values."
                                                )
                                        );
                                        return 0;
                                    }

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "Applied " + count
                                                            + " configured gamerule changes to this world."
                                            ),
                                            true
                                    );
                                    return count;
                                }))
        );
    }
}