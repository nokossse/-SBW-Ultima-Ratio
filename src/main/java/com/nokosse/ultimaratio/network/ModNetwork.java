package com.nokosse.ultimaratio.network;

import com.nokosse.ultimaratio.UltimaRatio;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = UltimaRatio.MODID)
public final class ModNetwork {
    private ModNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        r.playToServer(VbmrDecoyMessage.TYPE, VbmrDecoyMessage.CODEC, VbmrDecoyMessage::handle);
        r.playToServer(VsrReleaseMessage.TYPE, VsrReleaseMessage.CODEC, VsrReleaseMessage::handle);
        r.playToServer(VsrMarkMessage.TYPE, VsrMarkMessage.CODEC, VsrMarkMessage::handle);
        r.playToServer(DroneSelectMessage.TYPE, DroneSelectMessage.CODEC, DroneSelectMessage::handle);
        r.playToClient(GcsListMessage.TYPE, GcsListMessage.CODEC, GcsListMessage::handle);
        r.playToServer(GcsSelectMessage.TYPE, GcsSelectMessage.CODEC, GcsSelectMessage::handle);
        r.playToServer(GcsRefreshMessage.TYPE, GcsRefreshMessage.CODEC, GcsRefreshMessage::handle);
        r.playToClient(GcsPilotMessage.TYPE, GcsPilotMessage.CODEC, GcsPilotMessage::handle);
        r.playToServer(ReaperAimMessage.TYPE, ReaperAimMessage.CODEC, ReaperAimMessage::handle);
    }
}
