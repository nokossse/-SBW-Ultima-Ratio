package com.nokosse.ultimaratio.network;

import com.nokosse.ultimaratio.event.VsrRemote;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The operator pressed SBW's dismount key while flying a drone from a GCS: let go of the controls. */
public class VsrReleaseMessage {
    public static void encode(VsrReleaseMessage message, FriendlyByteBuf buf) {
    }

    public static VsrReleaseMessage decode(FriendlyByteBuf buf) {
        return new VsrReleaseMessage();
    }

    public static void handle(VsrReleaseMessage message, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                VsrRemote.releaseGcs(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
