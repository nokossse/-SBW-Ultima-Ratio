package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.resource.model.BlockModelReloadListener;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakedBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.block.GcsBlock;
import com.nokosse.ultimaratio.block.entity.GcsBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.resources.ResourceLocation;

public class GcsBlockEntityRenderer implements BlockEntityRenderer<GcsBlockEntity> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(UltimaRatio.MODID, "textures/bedrock/block/gcs.png");
    private static final ResourceLocation MODEL = new ResourceLocation(UltimaRatio.MODID, "models/bedrock/block/gcs.geo.json");

    @Override
    public int getViewDistance() {
        return 256;
    }

    /** The model is 2 blocks long and tall: the default box of one block would cull it too early. */
    @Override
    public boolean shouldRenderOffScreen(GcsBlockEntity be) {
        return true;
    }

    private static void show(GcsBlockEntity be, String bone, boolean visible) {
        BoneState state = be.modelInstance.getBone(bone);
        if (state != null) {
            state.visible = visible;
        }
    }

    @Override
    public void render(GcsBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (be.getBlockState().getValue(GcsBlock.SECOND)) return;
        BakedBedrockModel model = BlockModelReloadListener.INSTANCE.getModel(MODEL);
        if (model == null) return;
        if (be.modelInstance == null || be.modelInstance.baseModel() != model) be.modelInstance = model.createInstance();

        // idle: both screens off; flying: left one on, right one shows the drone type (GcsBlock.SCREEN)
        int screen = be.getBlockState().getValue(GcsBlock.SCREEN);
        show(be, "move_scr_left_off", screen == 0);
        show(be, "move_scr_right_off", screen == 0);
        show(be, "move_scr_left_on", screen != 0);
        show(be, "move_scr_right_reap", screen == 1);
        show(be, "move_scr_right_vsr", screen == 2);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        // keyboard (-z of the model) toward the player who placed it: the opposite of GF300's 180 - yRot
        poseStack.mulPose(Axis.YP.rotationDegrees(-be.getBlockState().getValue(GcsBlock.FACING).toYRot()));
        be.modelInstance.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), packedLight, packedOverlay);
        poseStack.popPose();
    }
}
