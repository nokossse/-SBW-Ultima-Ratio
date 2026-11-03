package com.nokosse.ultimaratio.network;

import com.nokosse.ultimaratio.event.VsrRemote;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Client to server, once a second while the GCS list is open: send the drones again (distances move). */
public class GcsRefreshMessage {
    private final BlockPos pos;

    public GcsRefreshMessage(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(GcsRefreshMessage message, FriendlyByteBuf buf) {
        buf.writeBlockPos(message.pos);
    }

    public static GcsRefreshMessage decode(FriendlyByteBuf buf) {
        return new GcsRefreshMessage(buf.readBlockPos());
    }

    public static void handle(GcsRefreshMessage message, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                VsrRemote.refreshGcs(player, message.pos);
            }
        });
        ctx.setPacketHandled(true);
    }
}
