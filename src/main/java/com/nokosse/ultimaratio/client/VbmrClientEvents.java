package com.nokosse.ultimaratio.client;

import com.atsuishio.superbwarfare.init.ModKeyMappings;
import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.entity.VbmrEntity;
import com.nokosse.ultimaratio.entity.VbmrMepacEntity;
import com.nokosse.ultimaratio.network.VbmrDecoyMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = UltimaRatio.MODID, value = Dist.CLIENT)
public class VbmrClientEvents {
    private static boolean decoyWasDown;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        boolean down = ModKeyMappings.RELEASE_DECOY.isDown();
        boolean pressed = down && !decoyWasDown;
        decoyWasDown = down;
        if (!pressed) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        if (player.getVehicle() instanceof VbmrMepacEntity mepac) {
            if (mepac.getSeatIndex(player) == VbmrMepacEntity.MACHINE_GUN_SEAT) {
                PacketDistributor.sendToServer(new VbmrDecoyMessage());
            }
            return;
        }
        if (!(player.getVehicle() instanceof VbmrEntity vbmr)) {
            return;
        }
        if (vbmr.getSeatIndex(player) != vbmr.getTurretControllerIndex()) {
            return;
        }
        PacketDistributor.sendToServer(new VbmrDecoyMessage());
    }
}
