package com.nokosse.ultimaratio.init;

import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.client.renderer.Aster30MissileRenderer;
import com.nokosse.ultimaratio.client.renderer.MeteorRenderer;
import com.nokosse.ultimaratio.client.renderer.MicaRenderer;
import com.nokosse.ultimaratio.client.renderer.ScalpEgRenderer;
import com.nokosse.ultimaratio.client.renderer.CaesarRenderer;
import com.nokosse.ultimaratio.client.renderer.Gf300BlockEntityRenderer;
import com.nokosse.ultimaratio.client.renderer.HadesMissileRenderer;
import com.nokosse.ultimaratio.client.renderer.HadesRenderer;
import com.nokosse.ultimaratio.client.renderer.KeraxRenderer;
import com.nokosse.ultimaratio.client.renderer.LeclercAzurRenderer;
import com.nokosse.ultimaratio.client.renderer.LeclercXlrRenderer;
import com.nokosse.ultimaratio.client.renderer.MambaRadarRenderer;
import com.nokosse.ultimaratio.client.renderer.MambaRenderer;
import com.nokosse.ultimaratio.client.renderer.RafaleRenderer;
import com.nokosse.ultimaratio.client.renderer.SampTNgRenderer;
import com.nokosse.ultimaratio.client.renderer.ThundartRenderer;
import com.nokosse.ultimaratio.client.renderer.ThundartRocketRenderer;
import com.nokosse.ultimaratio.client.renderer.TigreRenderer;
import com.nokosse.ultimaratio.client.renderer.VbmrMepacRenderer;
import com.nokosse.ultimaratio.client.renderer.VbmrRenderer;
import com.nokosse.ultimaratio.entity.projectile.HadesMissileEntity;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = UltimaRatio.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModEntityRenderers {
    @SubscribeEvent
    public static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CAESAR.get(), CaesarRenderer::new);
        event.registerEntityRenderer(ModEntities.MAMBA.get(), MambaRenderer::new);
        event.registerEntityRenderer(ModEntities.HADES.get(), HadesRenderer::new);
        event.registerEntityRenderer(ModEntities.MAMBA_RADAR.get(), MambaRadarRenderer::new);
        event.registerEntityRenderer(ModEntities.SAMP_T_NG.get(), SampTNgRenderer::new);
        event.registerEntityRenderer(ModEntities.THUNDART.get(), ThundartRenderer::new);
        event.registerEntityRenderer(ModEntities.RENAULT_KERAX.get(), KeraxRenderer::new);
        event.registerEntityRenderer(ModEntities.VBMR.get(), VbmrRenderer::new);
        event.registerEntityRenderer(ModEntities.SERVAL.get(), VbmrRenderer::new);
        event.registerEntityRenderer(ModEntities.SERVAL_DSA.get(), VbmrRenderer::new);
        event.registerEntityRenderer(ModEntities.VBMR_MEPAC.get(), VbmrMepacRenderer::new);
        event.registerEntityRenderer(ModEntities.LECLERC_XLR.get(), LeclercXlrRenderer::new);
        event.registerEntityRenderer(ModEntities.LECLERC_AZUR.get(), LeclercAzurRenderer::new);
        event.registerEntityRenderer(ModEntities.RAFALE_C.get(), RafaleRenderer::new);
        event.registerEntityRenderer(ModEntities.TIGRE.get(), TigreRenderer::new);
        event.registerEntityRenderer(ModEntities.ASTER_30_MISSILE.get(), Aster30MissileRenderer::new);
        event.registerEntityRenderer(ModEntities.MICA.get(), MicaRenderer::new);
        event.registerEntityRenderer(ModEntities.METEOR.get(), MeteorRenderer::new);
        event.registerEntityRenderer(ModEntities.SCALP_EG.get(), ScalpEgRenderer::new);
        event.registerEntityRenderer(ModEntities.THUNDART_ROCKET.get(), ThundartRocketRenderer::new);
        event.registerEntityRenderer(ModEntities.HADES_MISSILE.get(), HadesMissileRenderer::new);
        event.registerBlockEntityRenderer(ModBlocks.GF300_BE.get(), ctx -> new Gf300BlockEntityRenderer());
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<Unit>() {
            @Override
            protected Unit prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
                return Unit.INSTANCE;
            }

            @Override
            protected void apply(Unit object, ResourceManager resourceManager, ProfilerFiller profiler) {
                HadesMissileEntity.ensureClientAssets();
            }
        });
    }
}
