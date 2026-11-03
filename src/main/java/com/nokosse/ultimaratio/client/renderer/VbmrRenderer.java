package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.nokosse.ultimaratio.entity.VbmrEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

public class VbmrRenderer extends GeoVehicleRenderer<VbmrEntity> {
    private static final float VOLANT_TURNS = 12.0F;

    public VbmrRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean hideForTurretControllerWhileZooming() {
        return true;
    }

    @Override
    public void transformCustomModelPart(
            VbmrEntity entity,
            VehicleModelInstance instance,
            PoseStack poseStack,
            float entityYaw,
            float partialTicks
    ) {
        super.transformCustomModelPart(entity, instance, poseStack, entityYaw, partialTicks);
        float steer = Mth.lerp(partialTicks, entity.getRudderRotO(), entity.getRudderRot());
        BoneState volant = instance.getBone("volant");
        if (volant != null && volant.rotation != null) {
            volant.rotation.set(volant.definition().bindRotation());
            this.spinVolant(volant, VOLANT_TURNS * steer);
        }
    }

    /** Steering wheel axis in the model: Z for the tilted Serval/Griffon wheels. */
    protected void spinVolant(BoneState volant, float angle) {
        volant.rotation.rotateZ(angle);
    }
}
