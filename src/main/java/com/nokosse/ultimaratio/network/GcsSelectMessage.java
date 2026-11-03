package com.nokosse.ultimaratio.network;

import com.nokosse.ultimaratio.event.VsrRemote;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.nokosse.ultimaratio.UltimaRatio;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Client to server: fly this drone from this GCS (checked again by VsrRemote). */
public class GcsSelectMessage implements CustomPacketPayload {
    public static final Type<GcsSelectMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(UltimaRatio.MODID, "gcs_select"));
    public static final StreamCodec<FriendlyByteBuf, GcsSelectMessage> CODEC = StreamCodec.of((buf, m) -> encode(m, buf), GcsSelectMessage::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

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

    public static void handle(GcsSelectMessage message, IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        if (player != null) {
            VsrRemote.selectFromGcs(player, message.pos, message.drone);
        }
    }
}
