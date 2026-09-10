package com.nokosse.ultimaratio.client;

import com.atsuishio.superbwarfare.init.ModKeyMappings;
import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.entity.VbmrEntity;
import com.nokosse.ultimaratio.network.ModNetwork;
import com.nokosse.ultimaratio.network.VbmrDecoyMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UltimaRatio.MODID, value = Dist.CLIENT)
public class VbmrClientEvents {
    private static boolean decoyWasDown;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        boolean down = ModKeyMappings.RELEASE_DECOY.isDown();
        boolean pressed = down && !decoyWasDown;
        decoyWasDown = down;
        if (!pressed) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null || !(player.getVehicle() instanceof VbmrEntity vbmr)) {
            return;
        }
        if (vbmr.getSeatIndex(player) != vbmr.getTurretControllerIndex()) {
            return;
        }
        ModNetwork.CHANNEL.sendToServer(new VbmrDecoyMessage());
    }
}
