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
                    .displayItems((params, output) -> {
                        output.accept(ContainerBlockItem.createInstance(ModEntities.CAESAR.get()));
                        output.accept(ContainerBlockItem.createInstance(ModEntities.MAMBA.get()));
                        output.accept(ContainerBlockItem.createInstance(ModEntities.HADES.get()));
                        output.accept(ContainerBlockItem.createInstance(ModEntities.MAMBA_RADAR.get()));
                        output.accept(ContainerBlockItem.createInstance(ModEntities.SAMP_T_NG.get()));
                        output.accept(ContainerBlockItem.createInstance(ModEntities.THUNDART.get()));
                        output.accept(ContainerBlockItem.createInstance(ModEntities.RENAULT_KERAX.get()));
                        output.accept(ContainerBlockItem.createInstance(ModEntities.VBMR.get()));
                        output.accept(ContainerBlockItem.createInstance(ModEntities.VBMR_MEPAC.get()));
                        output.accept(ContainerBlockItem.createInstance(ModEntities.SERVAL.get()));
                        output.accept(ContainerBlockItem.createInstance(ModEntities.SERVAL_DSA.get()));
                        output.accept(ContainerBlockItem.createInstance(ModEntities.SERVAL_LAD.get()));
                        output.accept(ContainerBlockItem.createInstance(ModEntities.LECLERC_XLR.get()));
                        output.accept(ContainerBlockItem.createInstance(ModEntities.LECLERC_AZUR.get()));
                        output.accept(ContainerBlockItem.createInstance(ModEntities.RAFALE_C.get()));
                        output.accept(ContainerBlockItem.createInstance(ModEntities.TIGRE.get()));
                        output.accept(ContainerBlockItem.createInstance(ModEntities.AS350.get()));
                        output.accept(ContainerBlockItem.createInstance(ModEntities.VSR700.get()));
                        output.accept(ModBlocks.VSR_MONITOR.get());
                        output.accept(ModBlocks.GF300_ITEM.get());
                    })
                    .build()
    );
}
