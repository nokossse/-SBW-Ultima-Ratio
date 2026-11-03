package com.nokosse.ultimaratio.network;

import com.nokosse.ultimaratio.event.VsrRemote;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.nokosse.ultimaratio.UltimaRatio;
import net.minecraft.server.level.ServerPlayer;


/** The tactical map is about to offer "cruise here" for this Reaper (selected, not flown): SBW's loiter packet that follows drives it. */
public class DroneSelectMessage implements CustomPacketPayload {
    public static final Type<DroneSelectMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(UltimaRatio.MODID, "drone_select"));
    public static final StreamCodec<FriendlyByteBuf, DroneSelectMessage> CODEC = StreamCodec.of((buf, m) -> encode(m, buf), DroneSelectMessage::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

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

    public static void handle(DroneSelectMessage message, IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        if (player != null) {
            VsrRemote.selectOnMap(player, message.entityId);
        }
    }
}
