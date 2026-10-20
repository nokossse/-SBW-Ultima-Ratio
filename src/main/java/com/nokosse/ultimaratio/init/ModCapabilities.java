package com.nokosse.ultimaratio.init;

import com.nokosse.ultimaratio.UltimaRatio;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = UltimaRatio.MODID)
public class ModCapabilities {
    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ModBlocks.GF300_BE.get(),
                (be, side) -> be.getEnergyCapability());
    }
}
