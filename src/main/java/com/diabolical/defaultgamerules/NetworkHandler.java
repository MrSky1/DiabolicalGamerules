package com.diabolical.defaultgamerules;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class NetworkHandler {
    private NetworkHandler() {}

    public record OpenPrompt(boolean realPrompt, boolean testPrompt) implements CustomPacketPayload {
        public static final Type<OpenPrompt> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DiabolicalDefaultGamerules.MOD_ID, "open_old_world_prompt"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenPrompt> STREAM_CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, OpenPrompt::realPrompt, ByteBufCodecs.BOOL, OpenPrompt::testPrompt, OpenPrompt::new);
        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record PromptResponse(boolean yes, boolean dontAskAgain, boolean realPrompt, boolean testPrompt) implements CustomPacketPayload {
        public static final Type<PromptResponse> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(DiabolicalDefaultGamerules.MOD_ID, "old_world_prompt_response"));
        public static final StreamCodec<RegistryFriendlyByteBuf, PromptResponse> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL, PromptResponse::yes,
                        ByteBufCodecs.BOOL, PromptResponse::dontAskAgain,
                        ByteBufCodecs.BOOL, PromptResponse::realPrompt,
                        ByteBufCodecs.BOOL, PromptResponse::testPrompt,
                        PromptResponse::new
                );
        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToClient(OpenPrompt.TYPE, OpenPrompt.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
                        ClientPromptHandler.open(payload.realPrompt(), payload.testPrompt());
                    }
                }));
        registrar.playToServer(PromptResponse.TYPE, PromptResponse.STREAM_CODEC, (payload, context) ->
                context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) ServerLevelAccess.handle(player, payload);
                }));
    }

    public static void openPrompt(ServerPlayer player, boolean realPrompt, boolean testPrompt) {
        PacketDistributor.sendToPlayer(player, new OpenPrompt(realPrompt, testPrompt));
    }

    public static void respond(boolean yes, boolean dontAskAgain, boolean realPrompt, boolean testPrompt) {
        PacketDistributor.sendToServer(new PromptResponse(yes, dontAskAgain, realPrompt, testPrompt));
    }
}