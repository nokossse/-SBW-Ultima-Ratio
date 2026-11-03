package com.nokosse.ultimaratio.client;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.enums.config.EDhApiDepthDirection;
import com.seibel.distanthorizons.api.enums.config.EDhApiDepthRange;
import com.seibel.distanthorizons.api.interfaces.render.IDhApiRenderProxy;
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeRenderEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam;
import com.seibel.distanthorizons.api.objects.DhApiResult;
import com.seibel.distanthorizons.api.objects.math.DhApiMat4f;
import com.nokosse.ultimaratio.UltimaRatio;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

/**
 * Distance to what Distant Horizons drew at the centre of the screen, read from its depth texture: what the operator
 * sees, at once (no terrain data to load). Only loaded when DH is installed, only called on the render thread.
 * shortcut: one blocking glReadPixels per frame while the laser view is up; move to a PBO read if it ever shows in a profile.
 */
final class DhDepth {
    /** Nothing drawn at the centre (sky). */
    static final double SKY = Double.POSITIVE_INFINITY;
    /** DH's picture cannot be read (no depth texture, shader pack, not rendered yet): use the terrain data instead. */
    static final double UNKNOWN = Double.NaN;

    private static boolean registered;
    private static boolean failureLogged;
    /** From DH's projection matrix, refreshed every frame: depth row (m22, constant) and the sign of the w row. */
    private static volatile float projZ = Float.NaN;
    private static volatile float projConst = Float.NaN;
    private static volatile float projW = Float.NaN;
    private static int framebuffer;
    private static long lastLog;

    private DhDepth() {
    }

    private static void register() {
        registered = true;
        DhApi.events.bind(DhApiBeforeRenderEvent.class, new DhApiBeforeRenderEvent() {
            @Override
            public void beforeRender(DhApiCancelableEventParam<DhApiRenderParam> event) {
                DhApiMat4f p = event.value.dhProjectionMatrix;
                // Perspective: one of m23/m32 is the +-1 that copies view z into w, the other the depth constant
                // (which one depends on the matrix's row/column naming, so it is told apart by value)
                boolean wInRow3 = Math.abs(Math.abs(p.m32) - 1F) < 1e-3F;
                projZ = p.m22;
                projConst = wInRow3 ? p.m23 : p.m32;
                projW = wInRow3 ? p.m32 : p.m23;
            }
        });
    }

    /** Distance in blocks along the view's centre to DH's terrain drawn there this frame; SKY or UNKNOWN otherwise. */
    static double centreDistance() {
        try {
            if (!registered) {
                register();
                return UNKNOWN; // the projection arrives with DH's next frame
            }
            IDhApiRenderProxy render = DhApi.Delayed.renderProxy;
            if (render == null || Float.isNaN(projZ)) {
                return UNKNOWN;
            }
            DhApiResult<Integer> texture = render.getDhDepthTextureGlId();
            if (!texture.success || texture.payload == null || texture.payload <= 0) {
                return UNKNOWN;
            }
            float depth = readCentre(texture.payload);
            if (Float.isNaN(depth)) {
                return UNKNOWN;
            }
            EDhApiDepthDirection direction = render.getDepthDirection();
            if (Math.abs(depth - direction.farDepth) < 1e-7F) {
                return SKY;
            }
            double ndc = render.getDepthRange() == EDhApiDepthRange.NEG_ONE_TO_POS_ONE ? depth * 2.0 - 1.0 : depth;
            // clip.z = projZ * z + projConst, clip.w = projW * z, ndc = clip.z / clip.w
            double viewZ = projConst / (projW * ndc - projZ);
            double distance = Math.abs(viewZ);
            long now = System.nanoTime();
            if (now - lastLog > 2_000_000_000L) { // throttled: one read per frame
                lastLog = now;
                UltimaRatio.LOGGER.info("[UR laser/DH] depth {} -> {} m (proj {} {} {})", depth, Math.round(distance), projZ, projConst, projW);
            }
            return Double.isFinite(distance) && distance > 0 ? distance : UNKNOWN;
        } catch (RuntimeException e) {
            if (!failureLogged) {
                failureLogged = true;
                UltimaRatio.LOGGER.warn("[UR laser/DH] reading Distant Horizons' depth failed, using its terrain data instead", e);
            }
            return UNKNOWN;
        }
    }

    /** Depth of the centre texel of DH's depth texture, through a framebuffer of our own (bindings restored). */
    private static float readCentre(int texture) {
        int previousRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int previousTexture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, previousTexture);
        if (framebuffer == 0) {
            framebuffer = GL30.glGenFramebuffers();
        }
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, framebuffer);
        try {
            GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, texture, 0);
            if (width <= 0 || height <= 0
                    || GL30.glCheckFramebufferStatus(GL30.GL_READ_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
                return Float.NaN;
            }
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer pixel = stack.mallocFloat(1);
                GL11.glReadPixels(width / 2, height / 2, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, pixel);
                return pixel.get(0);
            }
        } finally {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, previousRead);
        }
    }
}
