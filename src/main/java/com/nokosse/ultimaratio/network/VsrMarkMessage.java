package com.nokosse.ultimaratio.network;

import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.misc.ArtilleryIndicatorItem;
import com.atsuishio.superbwarfare.item.misc.FiringParametersItem;
import com.atsuishio.superbwarfare.item.misc.FiringParametersItemKt;
import com.nokosse.ultimaratio.entity.Vsr700Entity;
import com.nokosse.ultimaratio.event.VsrRemote;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Middle click while flying a VSR700 with an artillery indicator (or firing parameters) in the offhand: the
 * indicator's target is set on the cursor, as SBW's drone monitor does.
 */
public record VsrMarkMessage(double x, double y, double z) {
    /** Largest distance between the drone and the marked point the server accepts. */
    private static final double MAX_DISTANCE = 600.0;

    public static void encode(VsrMarkMessage message, FriendlyByteBuf buf) {
        buf.writeDouble(message.x);
        buf.writeDouble(message.y);
        buf.writeDouble(message.z);
    }

    public static VsrMarkMessage decode(FriendlyByteBuf buf) {
        return new VsrMarkMessage(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(VsrMarkMessage message, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            Vsr700Entity vsr = player == null ? null : VsrRemote.flying(player);
            ItemStack offhand = player == null ? ItemStack.EMPTY : player.getOffhandItem();
            if (vsr == null || !(offhand.is(ModItems.ARTILLERY_INDICATOR.get()) || offhand.is(ModItems.FIRING_PARAMETERS.get()))
                    || !Double.isFinite(message.x + message.y + message.z)
                    || vsr.distanceToSqr(message.x, message.y, message.z) > MAX_DISTANCE * MAX_DISTANCE) {
                return;
            }
            BlockPos pos = BlockPos.containing(message.x, message.y, message.z);
            FiringParametersItem.Parameters old = FiringParametersItemKt.getFiringParameters(offhand);
            FiringParametersItemKt.setFiringParameters(offhand, new FiringParametersItem.Parameters(pos, old.radius(), old.isDepressed()));
            player.displayClientMessage(Component.translatable("tips.superbwarfare.mortar.target_pos")
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal("[" + pos.getX() + "," + pos.getY() + "," + pos.getZ() + "]")), true);
            player.level().playSound(null, player.blockPosition(), ModSounds.CANNON_ZOOM_IN.get(), net.minecraft.sounds.SoundSource.PLAYERS, 2f, 1f);
            if (offhand.getItem() instanceof ArtilleryIndicatorItem indicator) {
                indicator.setTarget(offhand, player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
