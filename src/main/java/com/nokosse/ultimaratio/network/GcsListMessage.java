package com.nokosse.ultimaratio.network;

import com.nokosse.ultimaratio.client.screen.GcsScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.nokosse.ultimaratio.UltimaRatio;

import java.util.List;
import java.util.UUID;

/** Server to client: the drones this GCS can fly, to pick from (the client may not see them yet). */
public class GcsListMessage implements CustomPacketPayload {
    public static final Type<GcsListMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(UltimaRatio.MODID, "gcs_list"));
    public static final StreamCodec<FriendlyByteBuf, GcsListMessage> CODEC = StreamCodec.of((buf, m) -> encode(m, buf), GcsListMessage::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public record Entry(UUID drone, String name, int distance) {
    }

    private final BlockPos pos;
    private final List<Entry> drones;

    public GcsListMessage(BlockPos pos, List<Entry> drones) {
        this.pos = pos;
        this.drones = drones;
    }

    public static void encode(GcsListMessage message, FriendlyByteBuf buf) {
        buf.writeBlockPos(message.pos);
        buf.writeCollection(message.drones, (b, e) -> {
            b.writeUUID(e.drone());
            b.writeUtf(e.name());
            b.writeVarInt(e.distance());
        });
    }

    public static GcsListMessage decode(FriendlyByteBuf buf) {
        return new GcsListMessage(buf.readBlockPos(), buf.readList(b -> new Entry(b.readUUID(), b.readUtf(), b.readVarInt())));
    }

    public static void handle(GcsListMessage message, IPayloadContext ctx) {
        GcsScreen.open(message.pos, message.drones);
    }
}
