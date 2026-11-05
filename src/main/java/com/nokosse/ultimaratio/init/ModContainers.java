package com.nokosse.ultimaratio.init;

import com.atsuishio.superbwarfare.api.event.RegisterContainersEvent;
import com.nokosse.ultimaratio.UltimaRatio;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Every vehicle of the addon also gets its crate in SBW's own creative tab. */
@Mod.EventBusSubscriber(modid = UltimaRatio.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModContainers {
    @SubscribeEvent
    public static void onRegisterContainers(RegisterContainersEvent event) {
        for (var vehicle : ModEntities.VEHICLES) {
            EntityType<?> type = vehicle.get();
            event.add(type);
        }
    }
}
