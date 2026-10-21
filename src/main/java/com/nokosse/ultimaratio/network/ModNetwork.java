package com.nokosse.ultimaratio.network;

import com.nokosse.ultimaratio.UltimaRatio;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = UltimaRatio.MODID)
public final class ModNetwork {
    private ModNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(VbmrDecoyMessage.TYPE, VbmrDecoyMessage.CODEC, VbmrDecoyMessage::handle);
    }
}
