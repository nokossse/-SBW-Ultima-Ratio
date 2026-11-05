package com.nokosse.ultimaratio.network;

import com.nokosse.ultimaratio.event.VsrRemote;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.nokosse.ultimaratio.UltimaRatio;
import net.minecraft.server.level.ServerPlayer;


/** The operator pressed SBW's dismount key while flying a drone from a GCS: let go of the controls. */
public class VsrReleaseMessage implements CustomPacketPayload {
    public static final Type<VsrReleaseMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(UltimaRatio.MODID, "vsr_release"));
    public static final StreamCodec<FriendlyByteBuf, VsrReleaseMessage> CODEC = StreamCodec.of((buf, m) -> encode(m, buf), VsrReleaseMessage::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void encode(VsrReleaseMessage message, FriendlyByteBuf buf) {
    }

    public static VsrReleaseMessage decode(FriendlyByteBuf buf) {
        return new VsrReleaseMessage();
    }

    public static void handle(VsrReleaseMessage message, IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
        if (player == null) {
            return;
        }
        VsrRemote.releaseGcs(player);
    }
}
