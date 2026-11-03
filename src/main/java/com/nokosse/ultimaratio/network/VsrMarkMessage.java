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
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import com.nokosse.ultimaratio.UltimaRatio;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;


/**
 * Middle click while flying a VSR700 with an artillery indicator (or firing parameters) in the offhand: the
 * indicator's target is set on the cursor, as SBW's drone monitor does.
 */
public record VsrMarkMessage(double x, double y, double z) implements CustomPacketPayload {
    public static final Type<VsrMarkMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(UltimaRatio.MODID, "vsr_mark"));
    public static final StreamCodec<FriendlyByteBuf, VsrMarkMessage> CODEC = StreamCodec.of((buf, m) -> encode(m, buf), VsrMarkMessage::decode);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

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

    public static void handle(VsrMarkMessage message, IPayloadContext ctx) {
        ServerPlayer player = (ServerPlayer) ctx.player();
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
    }
}
