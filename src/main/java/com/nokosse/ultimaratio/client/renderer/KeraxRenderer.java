package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.mojang.blaze3d.vertex.PoseStack;
import com.nokosse.ultimaratio.entity.KeraxEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class KeraxRenderer extends TruckRenderer<KeraxEntity> {
    public KeraxRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void transformCustomModelPart(KeraxEntity entity, VehicleModelInstance instance, PoseStack poseStack,
                                         float entityYaw, float partialTicks) {
        super.transformCustomModelPart(entity, instance, poseStack, entityYaw, partialTicks);
        hideBones(instance, "hitboxes", "collision", "dmg");
        this.driveTruck(entity, instance, partialTicks, -1F, -1F);
    }
}
