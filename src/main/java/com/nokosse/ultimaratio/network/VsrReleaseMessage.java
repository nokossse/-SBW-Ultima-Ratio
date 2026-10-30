package com.nokosse.ultimaratio.network;

import com.nokosse.ultimaratio.item.VsrMonitorItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** The operator pressed SBW's dismount key while flying a VSR700: let go of the controls. */
public class VsrReleaseMessage {
    public static void encode(VsrReleaseMessage message, FriendlyByteBuf buf) {
    }

    public static VsrReleaseMessage decode(FriendlyByteBuf buf) {
        return new VsrReleaseMessage();
    }

    public static void handle(VsrReleaseMessage message, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof VsrMonitorItem) {
                stack.getOrCreateTag().putBoolean(VsrMonitorItem.USING, false);
            }
        });
        ctx.setPacketHandled(true);
    }
}
