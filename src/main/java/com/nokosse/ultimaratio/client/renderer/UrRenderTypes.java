package com.nokosse.ultimaratio.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class UrRenderTypes extends RenderType {
    private UrRenderTypes(String name, VertexFormat format, VertexFormat.Mode mode, int size, boolean crumbling, boolean sort, Runnable setup, Runnable clear) {
        super(name, format, mode, size, crumbling, sort, setup, clear);
    }

    /** Translucent entity quads that never write depth, so glass and HUD planes do not hide what is drawn after them. */
    public static final Function<ResourceLocation, RenderType> GLASS = Util.memoize(texture -> {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                .setTextureState(new TextureStateShard(texture, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(true);
        return RenderType.create("ultimaratio_glass", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, true, state);
    });

    /** Full-bright additive colour quads (vanilla lightning blend) for engine flames; no depth write so layers stack. */
    public static final RenderType AFTERBURNER = RenderType.create(
            "ultimaratio_afterburner",
            DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS,
            8192,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false)
    );
}
