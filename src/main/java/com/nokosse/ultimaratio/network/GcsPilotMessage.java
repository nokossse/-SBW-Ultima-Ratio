package com.nokosse.ultimaratio.network;

import com.nokosse.ultimaratio.client.VsrClientEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Server to client: the drone flown from a GCS (its UUID), or "" once the link ends. */
public class GcsPilotMessage {
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

    public static void handle(GcsPilotMessage message, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> VsrClientEvents.setGcsDrone(message.drone)));
        ctx.setPacketHandled(true);
    }
}
