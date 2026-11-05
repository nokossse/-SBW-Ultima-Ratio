package com.nokosse.ultimaratio.init;

import com.atsuishio.superbwarfare.item.container.ContainerBlockItem;
import com.nokosse.ultimaratio.UltimaRatio;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, UltimaRatio.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("item_group.ultimaratio.main"))
                    // the CAESAR's crate: drawn with its vehicle icon (UrContainerIcon), like in the tab itself
                    .icon(() -> ContainerBlockItem.createInstance(ModEntities.CAESAR.get()))
                    .displayItems((params, output) -> {
                        ModEntities.VEHICLES.forEach(vehicle -> output.accept(ContainerBlockItem.createInstance(vehicle.get())));
                        output.accept(ModBlocks.GCS_ITEM.get());
                        output.accept(ModBlocks.GF300_ITEM.get());
                    })
                    .build()
    );
}
