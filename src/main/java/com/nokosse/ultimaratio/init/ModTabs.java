package com.nokosse.ultimaratio.init;

import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.item.container.ContainerBlockItem;
import com.nokosse.ultimaratio.UltimaRatio;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, UltimaRatio.MODID);

    public static final RegistryObject<CreativeModeTab> MAIN = TABS.register(
            "main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("item_group.ultimaratio.main"))
                    .icon(() -> new ItemStack(ModItems.CONTAINER.get()))
                    .displayItems((params, output) ->
                            output.accept(ContainerBlockItem.createInstance(ModEntities.CAESAR.get())))
                    .build()
    );
}
