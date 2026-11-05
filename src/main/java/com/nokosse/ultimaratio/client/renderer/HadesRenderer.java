package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.mojang.blaze3d.vertex.PoseStack;
import com.nokosse.ultimaratio.entity.HadesEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class HadesRenderer extends TruckRenderer<HadesEntity> {
    public HadesRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void transformCustomModelPart(HadesEntity entity, VehicleModelInstance instance, PoseStack poseStack,
                                         float entityYaw, float partialTicks) {
        super.transformCustomModelPart(entity, instance, poseStack, entityYaw, partialTicks);
        this.driveTruck(entity, instance, partialTicks, -1F, -1F);
    }
}
