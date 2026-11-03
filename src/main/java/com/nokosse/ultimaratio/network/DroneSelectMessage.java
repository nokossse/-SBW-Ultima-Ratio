package com.nokosse.ultimaratio.network;

import com.nokosse.ultimaratio.event.VsrRemote;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The tactical map is about to offer "cruise here" for this Reaper (selected, not flown): SBW's loiter packet that follows drives it. */
public class DroneSelectMessage {
    private final int entityId;

    public DroneSelectMessage(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(DroneSelectMessage message, FriendlyByteBuf buf) {
        buf.writeInt(message.entityId);
    }

    public static DroneSelectMessage decode(FriendlyByteBuf buf) {
        return new DroneSelectMessage(buf.readInt());
    }

    public static void handle(DroneSelectMessage message, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                VsrRemote.selectOnMap(player, message.entityId);
            }
        });
        ctx.setPacketHandled(true);
    }
}
