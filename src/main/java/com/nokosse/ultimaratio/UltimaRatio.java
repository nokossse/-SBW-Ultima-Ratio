package com.nokosse.ultimaratio;

import com.mojang.logging.LogUtils;
import com.nokosse.ultimaratio.init.ModEntities;
import com.nokosse.ultimaratio.init.ModTabs;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(UltimaRatio.MODID)
public class UltimaRatio {
    public static final String MODID = "ultimaratio";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UltimaRatio(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        ModEntities.REGISTRY.register(bus);
        ModTabs.TABS.register(bus);
    }
}
