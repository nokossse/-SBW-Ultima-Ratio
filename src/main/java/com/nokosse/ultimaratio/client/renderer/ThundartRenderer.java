package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

public class ThundartRenderer extends TruckRenderer<VehicleEntity> {

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
    public void transformCustomModelPart(
            VehicleEntity entity,
            VehicleModelInstance instance,
            PoseStack poseStack,
            float entityYaw,
            float partialTicks
    ) {
        super.transformCustomModelPart(entity, instance, poseStack, entityYaw, partialTicks);
        hideBones(instance, "hitboxes", "hitbox_collision", "hitbox_damage");
        this.driveTruck(entity, instance, partialTicks, 1F, 1F);

        float pitch = Mth.clamp(
                -this.getTurretXRot(),
                entity.getTurretMinPitch(),
                entity.getTurretMaxPitch()
        ) * Mth.DEG_TO_RAD;
        BoneState barrel = instance.getBone("barrel");
        if (barrel != null && barrel.rotation != null) {
            barrel.rotation.rotationX(pitch);
            if (barrel.rotationInEuler != null) {
                barrel.rotationInEuler.set(pitch, 0.0F, 0.0F);
            }
        }
        this.aimCannonRams(instance, pitch);
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
}
