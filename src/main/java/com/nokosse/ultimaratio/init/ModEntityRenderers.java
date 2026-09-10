package com.nokosse.ultimaratio.init;

import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.client.renderer.CaesarRenderer;
import com.nokosse.ultimaratio.client.renderer.MambaRenderer;
import com.nokosse.ultimaratio.client.renderer.VbmrRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UltimaRatio.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEntityRenderers {
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CAESAR.get(), CaesarRenderer::new);
        event.registerEntityRenderer(ModEntities.MAMBA.get(), MambaRenderer::new);
        event.registerEntityRenderer(ModEntities.VBMR.get(), VbmrRenderer::new);
    }
}
