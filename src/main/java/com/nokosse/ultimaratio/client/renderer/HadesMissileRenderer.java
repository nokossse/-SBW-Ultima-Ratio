package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.renderer.projectile.BasicProjectileRenderer;
import com.nokosse.ultimaratio.entity.projectile.HadesMissileEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class HadesMissileRenderer extends BasicProjectileRenderer<HadesMissileEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("ultimaratio", "textures/bedrock/projectile/hades_missile.png");

    public HadesMissileRenderer(EntityRendererProvider.Context context) {
        super(context);
        HadesMissileEntity.ensureClientAssets();
    }

    @Override
    public ResourceLocation getTextureLocation(HadesMissileEntity entity) {
        return TEXTURE;
    }
}
