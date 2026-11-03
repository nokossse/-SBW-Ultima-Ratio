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


/** Client to server, once a second while the GCS list is open: send the drones again (distances move). */
public class GcsRefreshMessage implements CustomPacketPayload {
    public static final Type<GcsRefreshMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(UltimaRatio.MODID, "gcs_refresh"));
    public static final StreamCodec<FriendlyByteBuf, GcsRefreshMessage> CODEC = StreamCodec.of((buf, m) -> encode(m, buf), GcsRefreshMessage::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

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

    public static void handle(GcsRefreshMessage message, IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        if (player != null) {
            VsrRemote.refreshGcs(player, message.pos);
        }
    }
}
