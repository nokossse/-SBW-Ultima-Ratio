package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer;
import com.atsuishio.superbwarfare.resource.model.VehicleModelReloadListener;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakedBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.nokosse.ultimaratio.entity.RafaleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class RafaleRenderer extends GeoVehicleRenderer<RafaleEntity> {
    // Cockpit dashboard: separate model + x128 texture so the airframe texture stays small.
    private static final ResourceLocation COMMANDS_MODEL =
            ResourceLocation.fromNamespaceAndPath("ultimaratio", "models/bedrock/vehicle/rafale_commands.geo.json");
    private static final ResourceLocation COMMANDS_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("ultimaratio", "textures/bedrock/vehicle/rafale_commands.png");

    private static final String GLASS_BONE = "move_translucent";

    private BakedBedrockModel commandsBaked;
    private BakedModelInstance commandsInstance;

    public RafaleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void transformCustomModelPart(
            RafaleEntity entity,
            VehicleModelInstance instance,
            PoseStack poseStack,
            float entityYaw,
            float partialTicks
    ) {
        super.transformCustomModelPart(entity, instance, poseStack, entityYaw, partialTicks);

        BoneState glass = instance.getBone(GLASS_BONE);
        if (glass != null) {
            glass.visible = false;
        }

        if (entity.isWreck()) {
            return;
        }

        // Same flap channels and gains as SBW's A-10 renderer.
        this.flap(instance, "move_wingLR", 1.5F * Mth.lerp(partialTicks, entity.getFlap1LRotO(), entity.getFlap1LRot()));
        this.flap(instance, "move_wingRR", 1.5F * Mth.lerp(partialTicks, entity.getFlap1RRotO(), entity.getFlap1RRot()));
        this.flap(instance, "move_wingLR2", 1.5F * Mth.lerp(partialTicks, entity.getFlap1L2RotO(), entity.getFlap1L2Rot()));
        this.flap(instance, "move_wingRR2", 1.5F * Mth.lerp(partialTicks, entity.getFlap1R2RotO(), entity.getFlap1R2Rot()));
        this.flap(instance, "move_wingLB", Mth.lerp(partialTicks, entity.getFlap2LRotO(), entity.getFlap2LRot()));
        this.flap(instance, "move_wingRB", Mth.lerp(partialTicks, entity.getFlap2RRotO(), entity.getFlap2RRot()));

        float spin = Mth.rotLerp(partialTicks, entity.reactorSpinO, entity.reactorSpin) * Mth.DEG_TO_RAD;
        this.spin(instance, "move_reactor_left_rotation", spin);
        this.spin(instance, "move_reactor_right_rotation", -spin);

        // Rudder keeps its bind tilt (X 57.25) and swings around its own local Z axis.
        BoneState rudder = instance.getBone("move_weiyiL");
        if (rudder != null && rudder.rotation != null) {
            float yaw = Mth.clamp(Mth.lerp(partialTicks, entity.getFlap3RotO(), entity.getFlap3Rot()), -20F, 20F);
            rudder.rotation.set(rudder.definition().bindRotation());
            rudder.rotation.rotateZ(yaw * Mth.DEG_TO_RAD);
        }
    }

    @Override
    public void renderCustomPart(
            RafaleEntity entity,
            VehicleModelInstance instance,
            PoseStack poseStack,
            float entityYaw,
            float partialTicks,
            MultiBufferSource buffer,
            int packedLight
    ) {
        super.renderCustomPart(entity, instance, poseStack, entityYaw, partialTicks, buffer, packedLight);

        BakedBedrockModel baked = VehicleModelReloadListener.INSTANCE.getModel(COMMANDS_MODEL);
        if (baked != null) {
            if (baked != this.commandsBaked) {
                this.commandsBaked = baked;
                this.commandsInstance = baked.createInstance();
            }
            // Same pose stack as the airframe (vehicle axis + scale), so it sits exactly where it was in the Rafale model.
            this.commandsInstance.resetPose();
            this.hideBone(this.commandsInstance, GLASS_BONE);
            this.commandsInstance.renderToBuffer(
                    poseStack,
                    buffer,
                    RenderType.entityTranslucent(COMMANDS_TEXTURE),
                    BedrockModelRenderTypes.polyMeshCutout(COMMANDS_TEXTURE),
                    packedLight,
                    OverlayTexture.NO_OVERLAY
            );
        }

        // Glass and HUD planes last, in a translucent pass that does not write depth.
        this.glass(instance, this.getTextureLocation(entity), poseStack, buffer, packedLight);
        if (this.commandsInstance != null) {
            this.glass(this.commandsInstance, COMMANDS_TEXTURE, poseStack, buffer, packedLight);
        }
    }

    private void hideBone(BakedModelInstance instance, String name) {
        BoneState bone = instance.getBone(name);
        if (bone != null) {
            bone.visible = false;
        }
    }

    private void glass(BakedModelInstance instance, ResourceLocation texture, PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        BoneState bone = instance.getBone(GLASS_BONE);
        if (bone == null) {
            return;
        }
        bone.visible = true;
        instance.renderSingleBonePass(
                poseStack,
                instance.getIndex(GLASS_BONE),
                buffer.getBuffer(UrRenderTypes.GLASS.apply(texture)),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                1F, 1F, 1F, 1F,
                true,
                false
        );
        bone.visible = false;
    }

    private void spin(VehicleModelInstance instance, String name, float radians) {
        BoneState bone = instance.getBone(name);
        if (bone != null && bone.rotation != null) {
            bone.rotation.set(bone.definition().bindRotation());
            bone.rotation.rotateZ(radians);
        }
    }

    private void flap(VehicleModelInstance instance, String name, float degrees) {
        BoneState bone = instance.getBone(name);
        if (bone != null && bone.rotation != null) {
            bone.rotation.set(bone.definition().bindRotation());
            bone.rotation.rotateX(degrees * Mth.DEG_TO_RAD);
        }
    }
}
