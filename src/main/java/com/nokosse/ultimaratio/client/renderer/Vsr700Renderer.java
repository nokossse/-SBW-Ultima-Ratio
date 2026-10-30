package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.nokosse.ultimaratio.entity.Vsr700Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

public class Vsr700Renderer extends GeoVehicleRenderer<Vsr700Entity> {
    public Vsr700Renderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void transformCustomModelPart(
            Vsr700Entity entity,
            VehicleModelInstance instance,
            PoseStack poseStack,
            float entityYaw,
            float partialTicks
    ) {
        super.transformCustomModelPart(entity, instance, poseStack, entityYaw, partialTicks);
        if (entity.isWreck()) {
            return;
        }

        // Same rotor channels and gains as the AS350 / Tigre.
        float rot = Mth.lerp(partialTicks, entity.getPropellerRotO(), entity.getPropellerRot());
        BoneState propeller = instance.getBone("move_propeller");
        if (propeller != null && propeller.rotation != null) {
            propeller.rotation.rotateY(-rot);
        }
        BoneState tail = instance.getBone("move_tailPropeller");
        if (tail != null && tail.rotation != null) {
            tail.rotation.rotateX(6 * rot);
        }
        // Radar sensor: one turn every 2 s (9 deg/tick), slower than the radar sweep itself
        BoneState radar = instance.getBone("move_radar");
        if (radar != null && radar.rotation != null) {
            radar.rotation.rotateY((float) Math.toRadians(9.0 * (entity.tickCount + partialTicks)));
        }
    }
}
