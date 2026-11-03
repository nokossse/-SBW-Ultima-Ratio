package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.projectile.FastThrowableProjectile;
import com.atsuishio.superbwarfare.data.vehicle_skin.SkinInfo;
import com.atsuishio.superbwarfare.data.vehicle_skin.VehicleSkin;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nokosse.ultimaratio.entity.TigreEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;

public class TigreRenderer extends GeoVehicleRenderer<TigreEntity> {
    private static final String GLASS_BONE = "move_translucent";
    // Gunner (seat 1), 9M120 (weapon 1): SBW would show that missile on the dummy, we show a Kh-39 instead.
    private static final String KH39_DUMMY_BONE = "dummy_1_1_1";

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
        renderKh39Dummy(entity, instance, poseStack, entityYaw, partialTicks, buffer, packedLight);

        BoneState glass = instance.getBone(GLASS_BONE);
        if (glass == null) {
            return;
        }
        glass.visible = true;
        // The canopy has cubes and poly meshes; a bone pass draws only one kind.
        // The base renderer swaps the skin only for its own pass, so the glass follows the sprayed skin too.
        ResourceLocation texture = this.getTextureLocation(entity);
        SkinInfo skin = VehicleSkin.getSkin(entity);
        if (skin != null && ResourceLocation.tryParse(skin.getTexture()) != null) {
            texture = ResourceLocation.tryParse(skin.getTexture());
        }
        int index = instance.getIndex(GLASS_BONE);
        instance.renderSingleBonePass(poseStack, index, buffer.getBuffer(UrRenderTypes.GLASS.apply(texture)),
                packedLight, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F, true, false);
        instance.renderSingleBonePass(poseStack, index, buffer.getBuffer(UrRenderTypes.GLASS_POLY.apply(texture)),
                packedLight, OverlayTexture.NO_OVERLAY, 1F, 1F, 1F, 1F, false, false);
        glass.visible = false;
    }

    private void renderKh39Dummy(
            TigreEntity entity,
            VehicleModelInstance instance,
            PoseStack poseStack,
            float entityYaw,
            float partialTicks,
            MultiBufferSource buffer,
            int packedLight
    ) {
        GunData data = entity.getGunData(1, 1);
        BoneState bone = instance.getBone(KH39_DUMMY_BONE);
        if (entity.isWreck() || data == null || bone == null || data.ammo.get() <= 0) {
            return;
        }
        EntityType.byString("superbwarfare:kh_39").ifPresent(type -> {
            var missile = type.create(entity.level());
            if (missile == null) {
                return;
            }
            missile.tickCount = 1;
            if (missile instanceof FastThrowableProjectile p) {
                p.setSyncedTick(1); // the projectile renderer hides it while syncedTick is low
            }
            poseStack.pushPose();
            poseStack.mulPose(bone.getGlobalTransform(instance));
            var def = bone.definition();
            poseStack.translate(def.pivotX(), def.pivotY(), def.pivotZ());
            // Same scale and offset as the Kh-39 on SBW's Mi-28.
            poseStack.scale(0.75F, 0.75F, 0.75F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180F));
            this.entityRenderDispatcher.render(missile, 0, -0.385, -1.62, entityYaw, partialTicks, poseStack, buffer, packedLight);
            poseStack.popPose();
        });
    }
}
