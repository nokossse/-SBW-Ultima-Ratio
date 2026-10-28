package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.renderer.projectile.BasicProjectileRenderer;
import com.nokosse.ultimaratio.entity.projectile.ScalpEgEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class ScalpEgRenderer extends BasicProjectileRenderer<ScalpEgEntity> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("ultimaratio", "textures/bedrock/projectile/scalp_eg.png");

    public ScalpEgRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ScalpEgEntity entity) {
        return TEXTURE;
    }
}
