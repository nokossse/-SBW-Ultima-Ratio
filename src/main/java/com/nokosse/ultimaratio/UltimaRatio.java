package com.nokosse.ultimaratio;

import com.mojang.logging.LogUtils;
import com.nokosse.ultimaratio.init.ModBlocks;
import com.nokosse.ultimaratio.init.ModEntities;
import com.nokosse.ultimaratio.init.ModSounds;
import com.nokosse.ultimaratio.init.ModTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(UltimaRatio.MODID)
public class UltimaRatio {
    public static final String MODID = "ultimaratio";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UltimaRatio(IEventBus bus) {
        ModBlocks.register(bus);
        ModEntities.REGISTRY.register(bus);
        ModSounds.REGISTRY.register(bus);
        ModTabs.TABS.register(bus);
    }
}
