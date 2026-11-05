package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.nokosse.ultimaratio.entity.MambaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class MambaRenderer extends TruckRenderer<MambaEntity> {
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
    public void transformCustomModelPart(MambaEntity entity, VehicleModelInstance instance, PoseStack poseStack,
                                         float entityYaw, float partialTicks) {
        super.transformCustomModelPart(entity, instance, poseStack, entityYaw, partialTicks);
        this.driveTruck(entity, instance, partialTicks, -1F, -1F);

        // OFF = original bone, ON = green twin (fullbright).
        this.setVisible(instance, "move_auto_indicator", !entity.isAuto());
        this.setVisible(instance, "move_auto_indicator_on", entity.isAuto());

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

    private void setVisible(VehicleModelInstance instance, String name, boolean visible) {
        BoneState bone = instance.getBone(name);
        if (bone != null) {
            bone.visible = visible;
            bone.illuminated = visible && name.endsWith("_on");
        }
    }
}
