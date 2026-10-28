package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.renderer.projectile.BasicProjectileRenderer;
import com.atsuishio.superbwarfare.entity.projectile.FastThrowableProjectile;
import com.nokosse.ultimaratio.entity.projectile.Aster30MissileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public class Aster30MissileRenderer extends BasicProjectileRenderer<Aster30MissileEntity> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("ultimaratio", "textures/bedrock/projectile/aster_30_missile.png");

    private Entity dummyKh39;

    public Aster30MissileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** The silo dummy is a never-spawned entity: draw the Mi-28's KH-39 there, as SBW does for its own dummies. */
    @Override
    public void render(Aster30MissileEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        if (entity.isAddedToWorld()) {
            super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
            return;
        }
        if (this.dummyKh39 == null || this.dummyKh39.level() != entity.level()) {
            this.dummyKh39 = EntityType.byString("superbwarfare:kh_39")
                    .map(type -> type.create(entity.level())).orElse(null);
            if (this.dummyKh39 != null) {
                this.dummyKh39.tickCount = 1;
                if (this.dummyKh39 instanceof FastThrowableProjectile p) {
                    p.setSyncedTick(1);
                }
            }
        }
        if (this.dummyKh39 != null) {
            this.entityRenderDispatcher.render(this.dummyKh39, 0, 0, 0, yaw, partialTick, poseStack, buffer, packedLight);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(Aster30MissileEntity entity) {
        return TEXTURE;
    }
}
