package com.nokosse.ultimaratio.network;

import com.nokosse.ultimaratio.entity.VbmrEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class VbmrDecoyMessage {
    public static void encode(VbmrDecoyMessage message, FriendlyByteBuf buf) {
    }

    public static VbmrDecoyMessage decode(FriendlyByteBuf buf) {
        return new VbmrDecoyMessage();
    }

    public static void handle(VbmrDecoyMessage message, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            if (!(player.getVehicle() instanceof VbmrEntity vbmr)) {
                return;
            }
            if (vbmr.getSeatIndex(player) != vbmr.getTurretControllerIndex()) {
                return;
            }
            vbmr.setDecoyInputDown(true);
        });
        ctx.setPacketHandled(true);
    }
}
