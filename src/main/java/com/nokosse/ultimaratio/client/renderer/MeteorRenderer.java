package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.renderer.projectile.BasicProjectileRenderer;
import com.nokosse.ultimaratio.entity.projectile.MeteorEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class MeteorRenderer extends BasicProjectileRenderer<MeteorEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("ultimaratio", "textures/bedrock/projectile/meteor.png");

    public MeteorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(MeteorEntity entity) {
        return TEXTURE;
    }
}
