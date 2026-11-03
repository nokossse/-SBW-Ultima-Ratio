package com.nokosse.ultimaratio.client.renderer;

import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class VabPRenderer extends VbmrRenderer {
    public VabPRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    /** The VAB P steering wheel lies flat, so it turns around Y. */
    @Override
    protected void spinVolant(BoneState volant, float angle) {
        volant.rotation.rotateY(angle);
    }
}
