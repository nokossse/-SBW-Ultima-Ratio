package com.nokosse.ultimaratio.network;

import com.nokosse.ultimaratio.client.VsrClientEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.nokosse.ultimaratio.UltimaRatio;


/** Server to client: the drone flown from a GCS (its UUID), or "" once the link ends. The monitor keeps this in its NBT instead. */
public class GcsPilotMessage implements CustomPacketPayload {
    public static final Type<GcsPilotMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(UltimaRatio.MODID, "gcs_pilot"));
    public static final StreamCodec<FriendlyByteBuf, GcsPilotMessage> CODEC = StreamCodec.of((buf, m) -> encode(m, buf), GcsPilotMessage::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private final String drone;

    public GcsPilotMessage(String drone) {
        this.drone = drone;
    }

    public static void encode(GcsPilotMessage message, FriendlyByteBuf buf) {
        buf.writeUtf(message.drone);
    }

    public static GcsPilotMessage decode(FriendlyByteBuf buf) {
        return new GcsPilotMessage(buf.readUtf());
    }

    public static void handle(GcsPilotMessage message, IPayloadContext ctx) {
        VsrClientEvents.setGcsDrone(message.drone);
    }
}
