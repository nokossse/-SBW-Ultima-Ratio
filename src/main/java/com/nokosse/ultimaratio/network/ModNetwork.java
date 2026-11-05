package com.nokosse.ultimaratio.network;

import com.nokosse.ultimaratio.UltimaRatio;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(UltimaRatio.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );
    private static int nextId;

    private ModNetwork() {
    }

    /** Message ids follow this order: append new messages at the end. */
    public static void register() {
        toServer(VbmrDecoyMessage.class, VbmrDecoyMessage::encode, VbmrDecoyMessage::decode, VbmrDecoyMessage::handle);
        toServer(VsrReleaseMessage.class, VsrReleaseMessage::encode, VsrReleaseMessage::decode, VsrReleaseMessage::handle);
        toServer(VsrMarkMessage.class, VsrMarkMessage::encode, VsrMarkMessage::decode, VsrMarkMessage::handle);
        toServer(DroneSelectMessage.class, DroneSelectMessage::encode, DroneSelectMessage::decode, DroneSelectMessage::handle);
        toClient(GcsListMessage.class, GcsListMessage::encode, GcsListMessage::decode, GcsListMessage::handle);
        toServer(GcsSelectMessage.class, GcsSelectMessage::encode, GcsSelectMessage::decode, GcsSelectMessage::handle);
        toServer(GcsRefreshMessage.class, GcsRefreshMessage::encode, GcsRefreshMessage::decode, GcsRefreshMessage::handle);
        toClient(GcsPilotMessage.class, GcsPilotMessage::encode, GcsPilotMessage::decode, GcsPilotMessage::handle);
        toServer(ReaperAimMessage.class, ReaperAimMessage::encode, ReaperAimMessage::decode, ReaperAimMessage::handle);
    }

    private static <M> void toServer(Class<M> type, BiConsumer<M, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, M> decoder,
                                     BiConsumer<M, Supplier<NetworkEvent.Context>> handler) {
        CHANNEL.registerMessage(nextId++, type, encoder, decoder, handler, Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }

    private static <M> void toClient(Class<M> type, BiConsumer<M, FriendlyByteBuf> encoder, Function<FriendlyByteBuf, M> decoder,
                                     BiConsumer<M, Supplier<NetworkEvent.Context>> handler) {
        CHANNEL.registerMessage(nextId++, type, encoder, decoder, handler, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
