package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.nokosse.ultimaratio.entity.MambaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

public class MambaRenderer extends GeoVehicleRenderer<MambaEntity> {
    private static final float STEER_LOCK_MULT = 1.35F;
    private static final float VOLANT_TURNS = 12.0F;
    // Dummy JSON pivot is [0,0,0] so SBW's extra translate(pivot) is a no-op.
    // Runtime translation is in silo_entry space (pixels):
    //   Y = along the tube (entry -> exit). Too high = nose sticks out.
    //   Z = into the silo (up at rest, back when deployed).
    private static final float SILO_DUMMY_MID_Y = 42F;
    private static final float SILO_DUMMY_Z = -5F;

    public MambaRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean hideForTurretControllerWhileZooming() {
        return true;
    }

    @Override
    public void transformCustomModelPart(
            MambaEntity entity,
            VehicleModelInstance instance,
            PoseStack poseStack,
            float entityYaw,
            float partialTicks
    ) {
        super.transformCustomModelPart(entity, instance, poseStack, entityYaw, partialTicks);
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

        for (int i = 1; i <= 8; i++) {
            BoneState dummy = instance.getBone("dummy_2_0_" + i);
            if (dummy == null) {
                continue;
            }
            dummy.x = 0.0F;
            dummy.y = SILO_DUMMY_MID_Y;
            dummy.z = SILO_DUMMY_Z;
        }
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
