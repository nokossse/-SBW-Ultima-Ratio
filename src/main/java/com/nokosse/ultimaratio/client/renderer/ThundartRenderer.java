package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public class ThundartRenderer extends GeoVehicleRenderer<VehicleEntity> {
    private static final float STEER_LOCK_MULT = 1.35F;
    private static final float VOLANT_TURNS = 12.0F;

    /** Absolute Blockbench pivots (geo Y/Z). Rods extend along local -Z. */
    private static final float BARREL_Y = 34.2F;
    private static final float BARREL_Z = 59.1F;
    private static final float ROD_Y = 29.2F;
    private static final float ROD_Z = 34.4F;
    private static final float CYL_Y = 31.83397F;
    private static final float CYL_Z = 1.76603F;
    private static final float REL_Y = CYL_Y - BARREL_Y;
    private static final float REL_Z = CYL_Z - BARREL_Z;

    public ThundartRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean hideForTurretControllerWhileZooming() {
        return true;
    }

    @Override
    public void transformCustomModelPart(
            VehicleEntity entity,
            VehicleModelInstance instance,
            PoseStack poseStack,
            float entityYaw,
            float partialTicks
    ) {
        super.transformCustomModelPart(entity, instance, poseStack, entityYaw, partialTicks);
        this.hideBone(instance, "hitboxes");
        this.hideBone(instance, "hitbox_collision");
        this.hideBone(instance, "hitbox_damage");

        float steer = Mth.lerp(partialTicks, entity.getRudderRotO(), entity.getRudderRot());
        float lock = steer * STEER_LOCK_MULT;
        float leftSpin = this.getLeftWheelRot();
        float rightSpin = this.getRightWheelRot();
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

        float pitch = Mth.clamp(
                -this.getTurretXRot(),
                entity.getTurretMinPitch(),
                entity.getTurretMaxPitch()
        ) * 0.017453292f;
        BoneState barrel = instance.getBone("barrel");
        if (barrel != null && barrel.rotation != null) {
            barrel.rotation.rotationX(pitch);
            if (barrel.rotationInEuler != null) {
                barrel.rotationInEuler.set(pitch, 0.0F, 0.0F);
            }
        }
        this.aimCannonRams(instance, pitch);

        BoneState volant = instance.getBone("volant");
        if (volant != null && volant.rotation != null) {
            volant.rotation.set(volant.definition().bindRotation());
            volant.rotation.rotateZ(VOLANT_TURNS * steer);
        }
    }

    /**
     * FH77 / Caesar 2D look-at in the pitch plane between {@code verine_turret}
     * and {@code verrine_barrel}. Bones must be listed in {@code animation.rams}.
     */
    private void aimCannonRams(VehicleModelInstance instance, float pitch) {
        float cos = Mth.cos(pitch);
        float sin = Mth.sin(pitch);
        float dY = BARREL_Y + REL_Y * cos - REL_Z * sin - ROD_Y;
        float dZ = BARREL_Z + REL_Y * sin + REL_Z * cos - ROD_Z;
        float angle = (float) Math.atan2(dY, -dZ);

        this.setRamX(instance, "verine_turret", angle);
        this.setRamX(instance, "verrine_barrel", angle - pitch);
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

    private void hideBone(VehicleModelInstance instance, String name) {
        BoneState bone = instance.getBone(name);
        if (bone == null) {
            return;
        }
        bone.visible = false;
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
