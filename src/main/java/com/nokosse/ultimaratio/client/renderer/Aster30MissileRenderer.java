package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.renderer.projectile.BasicProjectileRenderer;
import com.nokosse.ultimaratio.entity.projectile.Aster30MissileEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class Aster30MissileRenderer extends BasicProjectileRenderer<Aster30MissileEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("ultimaratio", "textures/bedrock/projectile/aster_30_missile.png");

    public Aster30MissileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(Aster30MissileEntity entity) {
        return TEXTURE;
    }
}
