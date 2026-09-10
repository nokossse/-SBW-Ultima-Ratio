package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.nokosse.ultimaratio.entity.VbmrMepacEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

public class VbmrMepacRenderer extends GeoVehicleRenderer<VbmrMepacEntity> {
    private static final float VOLANT_TURNS = 12.0F;

    public VbmrMepacRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean hideForTurretControllerWhileZooming() {
        return true;
    }

    @Override
    public void transformCustomModelPart(
            VbmrMepacEntity entity,
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
            volant.rotation.rotateZ(VOLANT_TURNS * steer);
        }
        float deg = (float) Math.PI / 180.0F;
        BoneState turret = instance.getBone("turret");
        if (turret != null && turret.rotation != null) {
            turret.rotation.set(turret.definition().bindRotation());
            turret.rotation.rotateY(this.getTurretYRot() * deg);
        }
        BoneState barrel = instance.getBone("barrel");
        if (barrel != null && barrel.rotation != null) {
            barrel.rotation.set(barrel.definition().bindRotation());
            barrel.rotation.rotateX(this.getTurretXRot() * deg);
        }
        if (entity.getNthEntity(VbmrMepacEntity.MACHINE_GUN_SEAT) == null) {
            BoneState hmgYaw = instance.getBone("move_hmgYaw");
            if (hmgYaw != null && hmgYaw.rotation != null) {
                hmgYaw.rotation.set(hmgYaw.definition().bindRotation());
                hmgYaw.rotation.rotateY(entity.getMgYaw() * deg);
            }
            BoneState hmgPitch = instance.getBone("move_hmgPitch");
            if (hmgPitch != null && hmgPitch.rotation != null) {
                hmgPitch.rotation.set(hmgPitch.definition().bindRotation());
                hmgPitch.rotation.rotationX(entity.getMgPitch() * deg);
            }
        }
    }
}
