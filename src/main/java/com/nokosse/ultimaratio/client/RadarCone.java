package com.nokosse.ultimaratio.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;

/** SBW 1.21.1 left drawFilledSector empty (TODO upstream), so the green radar sweep cone never showed. */
public final class RadarCone {
    private RadarCone() {
    }

    public static void draw(GuiGraphics guiGraphics, float cx, float cy, float radius, float startDeg, float sweepDeg) {
        if (radius < 2f || sweepDeg <= 0f) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor(0f, 1f, 0f, 0.2f);

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(cx, cy, 0f);
        Matrix4f matrix = pose.last().pose();
        int steps = Math.max(3, Math.min(12, (int) (sweepDeg / 180f * (float) Math.PI * radius / 30f)));
        BufferBuilder buf = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION);
        for (int i = 0; i <= steps; i++) {
            double a = Math.toRadians(startDeg + sweepDeg * i / steps);
            buf.addVertex(matrix, (float) (Math.cos(a) * radius), (float) (Math.sin(a) * radius), 0f);
            buf.addVertex(matrix, 0f, 0f, 0f);
        }
        BufferUploader.drawWithShader(buf.buildOrThrow());
        pose.popPose();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }
}
