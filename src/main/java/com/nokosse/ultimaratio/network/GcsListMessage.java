package com.nokosse.ultimaratio.network;

import com.nokosse.ultimaratio.client.screen.GcsScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Server to client: the drones this GCS can fly, to pick from (the client may not see them yet). */
public class GcsListMessage {
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

    public static void handle(GcsListMessage message, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> GcsScreen.open(message.pos, message.drones)));
        ctx.setPacketHandled(true);
    }
}
