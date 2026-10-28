package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.nokosse.ultimaratio.entity.As350Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

public class As350Renderer extends GeoVehicleRenderer<As350Entity> {
    public As350Renderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void transformCustomModelPart(
            As350Entity entity,
            VehicleModelInstance instance,
            PoseStack poseStack,
            float entityYaw,
            float partialTicks
    ) {
        super.transformCustomModelPart(entity, instance, poseStack, entityYaw, partialTicks);
        if (entity.isWreck()) {
            return;
        }

        // Same rotor channels and gains as the Tigre / Mi-28.
        float rot = Mth.lerp(partialTicks, entity.getPropellerRotO(), entity.getPropellerRot());
        BoneState propeller = instance.getBone("move_propeller");
        if (propeller != null && propeller.rotation != null) {
            propeller.rotation.rotateY(-rot);
        }
        BoneState tail = instance.getBone("move_tailPropeller");
        if (tail != null && tail.rotation != null) {
            tail.rotation.rotateX(6 * rot);
        }
    }
}
