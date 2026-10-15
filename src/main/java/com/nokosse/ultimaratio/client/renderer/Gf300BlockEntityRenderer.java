package com.nokosse.ultimaratio.client.renderer;

import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakedBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.atsuishio.superbwarfare.resource.model.BlockModelReloadListener;
import com.maydaymemory.mae.basic.ArrayPoseBuilder;
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory;
import com.maydaymemory.mae.blend.EulerAdditiveBlender;
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.block.Gf300Block;
import com.nokosse.ultimaratio.block.entity.Gf300BlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class Gf300BlockEntityRenderer implements BlockEntityRenderer<Gf300BlockEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(UltimaRatio.MODID, "textures/bedrock/block/gf300.png");
    private static final ResourceLocation MODEL = new ResourceLocation(UltimaRatio.MODID, "models/bedrock/block/gf300.geo.json");
    private static final ResourceLocation ANIM = new ResourceLocation(UltimaRatio.MODID, "animations/bedrock/block/gf300.animation.json");
    private static final String DEPLOY = "deploy_radar";
    private static final EulerAdditiveBlender BLENDER =
            new SimpleEulerAdditiveBlender(new ZYXBoneTransformFactory(), ArrayPoseBuilder::new);

    @Override
    public void render(Gf300BlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        if (be.getLevel() == null || !Gf300Block.isController(state)) return;

        BakedBedrockModel model = BlockModelReloadListener.INSTANCE.getModel(MODEL);
        if (model == null) return;
        if (be.modelInstance == null || be.modelInstance.baseModel() != model) be.modelInstance = model.createInstance();
        var instance = be.modelInstance;

        boolean powered = state.getValue(Gf300Block.POWERED);
        BedrockAnimation deploy = null;
        List<BedrockAnimation> animations = BlockModelReloadListener.INSTANCE.getAnimation(ANIM);
        if (animations != null) {
            for (BedrockAnimation a : animations) {
                if (DEPLOY.equals(a.getName())) deploy = a;
            }
        }
        float length = deploy != null && deploy.getSpecifiedEndTimeS() > 0 ? deploy.getSpecifiedEndTimeS() : 3f;

        // deploiement : on joue deploy_radar a l'endroit (alimente) ou a l'envers (replie = fold_radar)
        float now = (be.getLevel().getGameTime() + partialTick) / 20f;
        if (be.deployTime < 0) {
            be.deployTime = powered ? length : 0;
            be.lastRenderTime = now;
        }
        float dt = Math.max(0, now - be.lastRenderTime);
        be.lastRenderTime = now;
        be.deployTime = Mth.clamp(be.deployTime + (powered ? dt : -dt), 0, length);

        instance.resetPose();
        if (deploy != null) {
            instance.applyPose(BLENDER.blend(instance.getBindPose(), deploy.evaluate(be.deployTime)));
        }

        BoneState rolling = instance.getBone("rolling");
        if (rolling != null && powered && be.deployTime >= length) {
            rolling.rotation.mul(Axis.YN.rotationDegrees((be.getTick() + partialTick) * Gf300BlockEntity.DEGREES_PER_TICK));
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(180f - state.getValue(Gf300Block.FACING).toYRot()));
        // l'origine du modele est entre la 3e et la 4e case de la longueur ; le controleur est la 4e
        poseStack.translate(0.0, 0.0, -0.5);

        instance.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityTranslucent(TEXTURE)), packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
