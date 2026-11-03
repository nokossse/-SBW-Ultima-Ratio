package com.nokosse.ultimaratio.network;

import com.nokosse.ultimaratio.entity.Mq9ReaperEntity;
import com.nokosse.ultimaratio.event.VsrRemote;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Every tick while flying an MQ-9: where the operator's camera is and looks, for the Hellfire to ride (Agm65Mixin). */
public record ReaperAimMessage(Vec3 from, Vec3 dir) {
    /** The camera sits on the plane (zoomed sensor) or a little behind it (third person). */
    private static final double MAX_CAMERA_DISTANCE = 64.0;

    public static void encode(ReaperAimMessage message, FriendlyByteBuf buf) {
        buf.writeDouble(message.from.x);
        buf.writeDouble(message.from.y);
        buf.writeDouble(message.from.z);
        buf.writeFloat((float) message.dir.x);
        buf.writeFloat((float) message.dir.y);
        buf.writeFloat((float) message.dir.z);
    }

    public static ReaperAimMessage decode(FriendlyByteBuf buf) {
        return new ReaperAimMessage(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
                new Vec3(buf.readFloat(), buf.readFloat(), buf.readFloat()));
    }

    public static void handle(ReaperAimMessage message, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || !(VsrRemote.flying(player) instanceof Mq9ReaperEntity reaper)) {
                return;
            }
            Vec3 from = message.from;
            Vec3 dir = message.dir;
            if (!Double.isFinite(from.x + from.y + from.z + dir.x + dir.y + dir.z) || dir.lengthSqr() < 0.25
                    || reaper.distanceToSqr(from) > MAX_CAMERA_DISTANCE * MAX_CAMERA_DISTANCE) {
                return;
            }
            reaper.setAim(from, dir.normalize());
        });
        ctx.setPacketHandled(true);
    }
}
