package com.nokosse.ultimaratio.network;

import com.nokosse.ultimaratio.event.VsrRemote;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/** Client to server: fly this drone from this GCS (checked again by VsrRemote). */
public class GcsSelectMessage {
    private final BlockPos pos;
    private final UUID drone;

    public GcsSelectMessage(BlockPos pos, UUID drone) {
        this.pos = pos;
        this.drone = drone;
    }

    public static void encode(GcsSelectMessage message, FriendlyByteBuf buf) {
        buf.writeBlockPos(message.pos);
        buf.writeUUID(message.drone);
    }

    public static GcsSelectMessage decode(FriendlyByteBuf buf) {
        return new GcsSelectMessage(buf.readBlockPos(), buf.readUUID());
    }

    public static void handle(GcsSelectMessage message, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                VsrRemote.selectFromGcs(player, message.pos, message.drone);
            }
        });
        ctx.setPacketHandled(true);
    }
}
