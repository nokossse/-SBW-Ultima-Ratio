package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.nokosse.ultimaratio.entity.CaesarEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

public class CaesarRenderer extends GeoVehicleRenderer<CaesarEntity> {
    /** SBW caps rudder at ~0.6 rad (~34°). Mild extra lock on the front wheels. */
    private static final float STEER_LOCK_MULT = 1.35F;
    private static final float VOLANT_TURNS = 8.0F;

    public CaesarRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean hideForTurretControllerWhileZooming() {
        return true;
    }

    @Override
    public void transformCustomModelPart(
            CaesarEntity entity,
            VehicleModelInstance instance,
            PoseStack poseStack,
            float entityYaw,
            float partialTicks
    ) {
        super.transformCustomModelPart(entity, instance, poseStack, entityYaw, partialTicks);
        float pitchDeg = Mth.clamp(
                -this.getTurretXRot(),
                entity.getTurretMinPitch(),
                entity.getTurretMaxPitch()
        );
        // Blockbench: +15° barrel → turret rams +20°, cannon rams −5°.
        float degToRad = (float) Math.PI / 180.0F;
        float baseExtra = pitchDeg * (20.0F / 15.0F) * degToRad;
        float cannonExtra = pitchDeg * (-5.0F / 15.0F) * degToRad;
        this.addRamPitch(instance, "verine_base_left", baseExtra);
        this.addRamPitch(instance, "verine_base_right", baseExtra);
        this.addRamPitch(instance, "verine_cannon_left", cannonExtra);
        this.addRamPitch(instance, "verine_cannon_right", cannonExtra);
        this.steerWheelsAndVolant(entity, instance, partialTicks);
    }

    private void steerWheelsAndVolant(CaesarEntity entity, VehicleModelInstance instance, float partialTicks) {
        float steer = Mth.lerp(partialTicks, entity.getRudderRotO(), entity.getRudderRot());
        float lock = steer * STEER_LOCK_MULT;
        for (BoneState bone : instance.getBoneGroups().getLeftWheelsTurn()) {
            bone.rotation.rotationY(lock);
            bone.rotation.rotateX(1.5F * this.getLeftWheelRot());
        }
        for (BoneState bone : instance.getBoneGroups().getRightWheelsTurn()) {
            bone.rotation.rotationY(lock);
            bone.rotation.rotateX(1.5F * this.getRightWheelRot());
        }

        BoneState volant = instance.getBone("volant");
        if (volant != null && volant.rotation != null) {
            volant.rotation.set(volant.definition().bindRotation());
            volant.rotation.rotateZ(VOLANT_TURNS * steer);
        }
    }

    /** Extra X on top of the bind pose (−34° rest), matching the Blockbench linkage. */
    private void addRamPitch(VehicleModelInstance instance, String boneName, float extraX) {
        BoneState bone = instance.getBone(boneName);
        if (bone == null || bone.rotation == null) {
            return;
        }
        bone.rotation.set(bone.definition().bindRotation());
        bone.rotation.rotateX(extraX);
    }
}
