package com.nokosse.ultimaratio.network;

import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.entity.VbmrEntity;
import com.nokosse.ultimaratio.entity.VbmrMepacEntity;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record VbmrDecoyMessage() implements CustomPacketPayload {
    public static final Type<VbmrDecoyMessage> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(UltimaRatio.MODID, "vbmr_decoy"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VbmrDecoyMessage> CODEC =
            StreamCodec.unit(new VbmrDecoyMessage());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(VbmrDecoyMessage message, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (player.getVehicle() instanceof VbmrMepacEntity mepac) {
            if (mepac.getSeatIndex(player) == VbmrMepacEntity.MACHINE_GUN_SEAT) {
                mepac.setDecoyInputDown(true);
            }
            return;
        }
        if (!(player.getVehicle() instanceof VbmrEntity vbmr)) {
            return;
        }
        if (vbmr.getSeatIndex(player) != vbmr.getTurretControllerIndex()) {
            return;
        }
        vbmr.setDecoyInputDown(true);
    }
}
