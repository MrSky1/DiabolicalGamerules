package com.diabolical.defaultgamerules;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@Mod(DiabolicalDefaultGamerules.MOD_ID)
public class DiabolicalDefaultGamerules {

    public static final String MOD_ID =
            "diabolical_default_gamerules";

    public DiabolicalDefaultGamerules(IEventBus modEventBus) {

        NeoForge.EVENT_BUS.addListener(
                this::onCreateSpawnPosition
        );

        NeoForge.EVENT_BUS.addListener(
                this::onRegisterCommands
        );

        GameruleDefaults.initialize();
    }

    private void onCreateSpawnPosition(
            LevelEvent.CreateSpawnPosition event
    ) {

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        // Only apply defaults to a newly initialized
        // Overworld, not existing worlds.
        if (!level.dimension().equals(Level.OVERWORLD)) {
            return;
        }

        if (event.getSettings().isInitialized()) {
            return;
        }

        GameruleDefaults.apply(level);
    }

    private void onRegisterCommands(
            RegisterCommandsEvent event
    ) {

        event.getDispatcher().register(
                Commands.literal("syncdefaultgamerules")
                        .requires(source ->
                                source.hasPermission(2))
                        .executes(context -> {

                            int count =
                                    GameruleDefaults.sync(
                                            context.getSource().getServer()
                                    );

                            context.getSource().sendSuccess(
                                    () -> Component.literal(
                                            "Synchronized " + count
                                            + " modified gamerules."
                                    ),
                                    true
                            );

                            return count;
                        })
        );
    }
}