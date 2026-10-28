package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.nokosse.ultimaratio.entity.TigreEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

public class TigreRenderer extends GeoVehicleRenderer<TigreEntity> {
    private static final String GLASS_BONE = "move_translucent";

    public TigreRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void transformCustomModelPart(
            TigreEntity entity,
            VehicleModelInstance instance,
            PoseStack poseStack,
            float entityYaw,
            float partialTicks
    ) {
        super.transformCustomModelPart(entity, instance, poseStack, entityYaw, partialTicks);

        // The canopy is drawn later in a translucent pass.
        BoneState glass = instance.getBone(GLASS_BONE);
        if (glass != null) {
            glass.visible = false;
        }

        if (entity.isWreck()) {
            return;
        }

        // Same rotor channels and gains as SBW's Mi-28 renderer.
        float rot = Mth.lerp(partialTicks, entity.getPropellerRotO(), entity.getPropellerRot());
        BoneState propeller = instance.getBone("move_propeller");
        if (propeller != null && propeller.rotation != null) {
            propeller.rotation.rotateY(-rot);
        }
        BoneState tail = instance.getBone("move_tailPropeller");
        if (tail != null && tail.rotation != null) {
            tail.rotation.rotateX(6 * rot);
        }
    }

    @Override
    public void renderCustomPart(
            TigreEntity entity,
            VehicleModelInstance instance,
            PoseStack poseStack,
            float entityYaw,
            float partialTicks,
            MultiBufferSource buffer,
            int packedLight
    ) {
        super.renderCustomPart(entity, instance, poseStack, entityYaw, partialTicks, buffer, packedLight);

        BoneState glass = instance.getBone(GLASS_BONE);
        if (glass == null) {
            return;
        }
        glass.visible = true;
        // The canopy has cubes and poly meshes; a bone pass draws only one kind.
        var texture = this.getTextureLocation(entity);
        int index = instance.getIndex(GLASS_BONE);
        instance.renderSingleBonePass(poseStack, index, buffer.getBuffer(UrRenderTypes.GLASS.apply(texture)),
                packedLight, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F, true, false);
        instance.renderSingleBonePass(poseStack, index, buffer.getBuffer(UrRenderTypes.GLASS_POLY.apply(texture)),
                packedLight, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F, false, false);
        glass.visible = false;
    }
}
