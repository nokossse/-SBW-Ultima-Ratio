package com.nokosse.ultimaratio.init;

import com.atsuishio.superbwarfare.api.event.RegisterContainersEvent;
import com.nokosse.ultimaratio.UltimaRatio;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Every vehicle of the addon also gets its crate in SBW's own creative tab. */
@EventBusSubscriber(modid = UltimaRatio.MODID)
public class ModContainers {
    @SubscribeEvent
    public static void onRegisterContainers(RegisterContainersEvent event) {
        for (var vehicle : ModEntities.VEHICLES) {
            EntityType<?> type = vehicle.get();
            event.add(type);
        }
    }
}
