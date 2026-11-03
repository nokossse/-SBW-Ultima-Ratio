package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.nokosse.ultimaratio.entity.Mq9ReaperEntity;
import com.nokosse.ultimaratio.entity.Vsr700Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

public class Mq9ReaperRenderer extends GeoVehicleRenderer<Mq9ReaperEntity> {
    public Mq9ReaperRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void transformCustomModelPart(
            Mq9ReaperEntity entity,
            VehicleModelInstance instance,
            PoseStack poseStack,
            float entityYaw,
            float partialTicks
    ) {
        super.transformCustomModelPart(entity, instance, poseStack, entityYaw, partialTicks);
        if (entity.isWreck()) {
            return;
        }

        // Same propeller channel as SBW's propeller planes (Ju 87, KV-16)
        BoneState propeller = instance.getBone("move_propeller");
        if (propeller != null && propeller.rotation != null) {
            propeller.rotation.rotateZ(Mth.lerp(partialTicks, entity.getPropellerRotO(), entity.getPropellerRot()));
        }

        // The sensor ball turns (move_kamera, yaw) and its camera tilts (move_cam, pitch) toward where the operator looks:
        // SBW's nacelle camera angles, zero outside it; only on the operator's own screen. Flip a sign if one turns backwards.
        if (entity == Vsr700Entity.clientPiloted) {
            BoneState ball = instance.getBone("move_kamera");
            if (ball != null && ball.rotation != null) {
                ball.rotation.rotateY((float) Math.toRadians(ClientMouseHandler.nacelleCameraYaw));
            }
            BoneState camera = instance.getBone("move_cam");
            if (camera != null && camera.rotation != null) {
                camera.rotation.rotateX((float) -Math.toRadians(ClientMouseHandler.nacelleCameraPitch));
            }
        }
    }
}
