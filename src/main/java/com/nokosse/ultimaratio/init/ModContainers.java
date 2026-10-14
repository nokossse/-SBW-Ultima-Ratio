package com.nokosse.ultimaratio.init;

import com.atsuishio.superbwarfare.api.event.RegisterContainersEvent;
import com.nokosse.ultimaratio.UltimaRatio;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UltimaRatio.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModContainers {
    @SubscribeEvent
    public static void onRegisterContainers(RegisterContainersEvent event) {
        event.add(ModEntities.CAESAR);
        event.add(ModEntities.MAMBA);
        event.add(ModEntities.HADES);
        event.add(ModEntities.MAMBA_RADAR);
        event.add(ModEntities.SAMP_T_NG);
        event.add(ModEntities.THUNDART);
        event.add(ModEntities.RENAULT_KERAX);
        event.add(ModEntities.VBMR);
        event.add(ModEntities.VBMR_MEPAC);
        event.add(ModEntities.LECLERC_XLR);
        event.add(ModEntities.LECLERC_AZUR);
    }
}
