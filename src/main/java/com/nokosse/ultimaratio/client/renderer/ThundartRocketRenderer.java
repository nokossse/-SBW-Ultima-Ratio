package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.renderer.projectile.BasicProjectileRenderer;
import com.nokosse.ultimaratio.entity.projectile.ThundartRocketEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class ThundartRocketRenderer extends BasicProjectileRenderer<ThundartRocketEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("ultimaratio", "textures/bedrock/projectile/thundart_rocket.png");

    public ThundartRocketRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(ThundartRocketEntity entity) {
        return TEXTURE;
    }
}
