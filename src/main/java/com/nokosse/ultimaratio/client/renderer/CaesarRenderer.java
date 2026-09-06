package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.nokosse.ultimaratio.entity.CaesarEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public class CaesarRenderer extends GeoVehicleRenderer<CaesarEntity> {
    /** SBW caps rudder at ~0.6 rad (~34°). Mild extra lock on the front wheels. */
    private static final float STEER_LOCK_MULT = 1.35F;
    private static final float VOLANT_TURNS = 8.0F;

    /**
     * Absolute Blockbench pivots (geo Y/Z). Same YZ for left and right rams.
     * Rods extend along local -Z, so look-at is {@code atan2(dY, -dZ)}.
     */
    private static final float BARREL_Y = 38.44F;
    private static final float BARREL_Z = 66.6F;
    private static final float ROD_Y = 32.99961F;
    private static final float ROD_Z = 46.18114F;
    private static final float CYL_Y = 48.72053F;
    private static final float CYL_Z = 22.74175F;
    private static final float REL_Y = CYL_Y - BARREL_Y;
    private static final float REL_Z = CYL_Z - BARREL_Z;

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
        float pitch = Mth.clamp(
                -this.getTurretXRot(),
                entity.getTurretMinPitch(),
                entity.getTurretMaxPitch()
        ) * ((float) Math.PI / 180.0F);
        this.aimCannonRams(instance, pitch);
        this.steerWheelsAndVolant(entity, instance, partialTicks);
    }

    /**
     * FH77-style 2D look-at in the pitch plane. Trailer stab rams stay on JSON.
     * Cannon ram bones must be listed in {@code animation.rams} or the baker folds them.
     */
    private void aimCannonRams(VehicleModelInstance instance, float pitch) {
        float cos = Mth.cos(pitch);
        float sin = Mth.sin(pitch);
        float dY = BARREL_Y + REL_Y * cos - REL_Z * sin - ROD_Y;
        float dZ = BARREL_Z + REL_Y * sin + REL_Z * cos - ROD_Z;
        float angle = (float) Math.atan2(dY, -dZ);

        this.setRamX(instance, "verine_base_left", angle);
        this.setRamX(instance, "verine_base_right", angle);
        this.setRamX(instance, "verine_cannon_left", angle - pitch);
        this.setRamX(instance, "verine_cannon_right", angle - pitch);
    }

    private void setRamX(VehicleModelInstance instance, String boneName, float xRad) {
        BoneState bone = instance.getBone(boneName);
        if (bone == null || bone.rotation == null) {
            return;
        }
        bone.rotation.rotationX(xRad);
        if (bone.rotationInEuler != null) {
            Vector3f bind = bone.definition().bindEulerRotation();
            bone.rotationInEuler.set(xRad, bind.y, bind.z);
        }
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
}
