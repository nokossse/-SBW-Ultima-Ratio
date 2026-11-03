package com.nokosse.ultimaratio.network;

import com.nokosse.ultimaratio.UltimaRatio;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(UltimaRatio.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    private ModNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(
                id++,
                VbmrDecoyMessage.class,
                VbmrDecoyMessage::encode,
                VbmrDecoyMessage::decode,
                VbmrDecoyMessage::handle
        );
        CHANNEL.registerMessage(
                id++,
                VsrReleaseMessage.class,
                VsrReleaseMessage::encode,
                VsrReleaseMessage::decode,
                VsrReleaseMessage::handle
        );
        CHANNEL.registerMessage(
                id++,
                VsrMarkMessage.class,
                VsrMarkMessage::encode,
                VsrMarkMessage::decode,
                VsrMarkMessage::handle
        );
        CHANNEL.registerMessage(
                id++,
                DroneSelectMessage.class,
                DroneSelectMessage::encode,
                DroneSelectMessage::decode,
                DroneSelectMessage::handle
        );
        CHANNEL.registerMessage(id++, GcsListMessage.class, GcsListMessage::encode, GcsListMessage::decode, GcsListMessage::handle);
        CHANNEL.registerMessage(id++, GcsSelectMessage.class, GcsSelectMessage::encode, GcsSelectMessage::decode, GcsSelectMessage::handle);
        CHANNEL.registerMessage(id++, GcsRefreshMessage.class, GcsRefreshMessage::encode, GcsRefreshMessage::decode, GcsRefreshMessage::handle);
        CHANNEL.registerMessage(id++, GcsPilotMessage.class, GcsPilotMessage::encode, GcsPilotMessage::decode, GcsPilotMessage::handle);
    }
}
