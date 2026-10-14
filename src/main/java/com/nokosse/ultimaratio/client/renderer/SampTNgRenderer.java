package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.nokosse.ultimaratio.entity.SampTNgEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

public class SampTNgRenderer extends GeoVehicleRenderer<SampTNgEntity> {
    private static final float STEER_LOCK_MULT = 1.35F;
    private static final float VOLANT_TURNS = 12.0F;

    public SampTNgRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean hideForTurretControllerWhileZooming() {
        return true;
    }

    @Override
    public void transformCustomModelPart(
            SampTNgEntity entity,
            VehicleModelInstance instance,
            PoseStack poseStack,
            float entityYaw,
            float partialTicks
    ) {
        super.transformCustomModelPart(entity, instance, poseStack, entityYaw, partialTicks);
        this.hideBone(instance, "hitboxes");
        this.hideBone(instance, "collision");
        this.hideBone(instance, "dmg");

        float steer = Mth.lerp(partialTicks, entity.getRudderRotO(), entity.getRudderRot());
        float lock = steer * STEER_LOCK_MULT;
        float leftSpin = -this.getLeftWheelRot();
        float rightSpin = -this.getRightWheelRot();
        for (BoneState bone : instance.getBoneGroups().getLeftWheels()) {
            if (bone != null && bone.rotation != null) {
                bone.rotation.rotationX(1.5F * leftSpin);
            }
        }
        for (BoneState bone : instance.getBoneGroups().getRightWheels()) {
            if (bone != null && bone.rotation != null) {
                bone.rotation.rotationX(1.5F * rightSpin);
            }
        }
        this.steerFront(instance.getBone("wheelL0Turn"), lock, leftSpin);
        this.steerFront(instance.getBone("wheelR0Turn"), lock, rightSpin);

        BoneState volant = instance.getBone("volant");
        if (volant != null && volant.rotation != null) {
            volant.rotation.set(volant.definition().bindRotation());
            volant.rotation.rotateZ(-VOLANT_TURNS * steer);
        }

        BoneState dish = instance.getBone("rolling");
        if (dish != null && dish.rotation != null) {
            dish.rotation.set(dish.definition().bindRotation());
            if (entity.isRadarDishSpinning()) {
                dish.rotation.rotateY(entity.getRadarDishYawRad(partialTicks));
            }
        }
    }

    private void hideBone(VehicleModelInstance instance, String name) {
        BoneState bone = instance.getBone(name);
        if (bone == null) {
            return;
        }
        bone.x = 0.0F;
        bone.y = -4096.0F;
        bone.z = 0.0F;
    }

    private void steerFront(BoneState bone, float steer, float spin) {
        if (bone == null || bone.rotation == null) {
            return;
        }
        bone.rotation.set(bone.definition().bindRotation());
        bone.rotation.rotateY(steer);
        bone.rotation.rotateX(1.5F * spin);
    }
}
