package com.nokosse.ultimaratio.client;

import com.atsuishio.superbwarfare.api.event.ClientVehicleFireEvent;
import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.entity.ThundartEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UltimaRatio.MODID, value = Dist.CLIENT)
public class ThundartClientEvents {
    @SubscribeEvent
    public static void onVehicleFire(ClientVehicleFireEvent event) {
        if (!(event.getVehicle() instanceof ThundartEntity thundart)) {
            return;
        }
        thundart.playClientShot(event.getIndex());
    }
}
