package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer;
import com.atsuishio.superbwarfare.resource.model.VehicleModelReloadListener;
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakedBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nokosse.ultimaratio.entity.RafaleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

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

        float reheat = Mth.lerp(partialTicks, entity.afterburnerO, entity.afterburner);
        if (!entity.isWreck() && reheat > 0.01F) {
            // Airflow stretches the plume a little at speed: +25% at 900 km/h.
            float speed = 1F + 0.25F * (float) Mth.clamp(entity.getAbsoluteSpeed() * 72 / 900.0, 0, 1);
            VertexConsumer vc = buffer.getBuffer(UrRenderTypes.AFTERBURNER);
            float time = entity.tickCount + partialTicks;
            this.afterburner(instance, "move_reactor_left_rotation", poseStack, vc, reheat, speed, time);
            this.afterburner(instance, "move_reactor_right_rotation", poseStack, vc, reheat, speed, time + 17.3F);
        }
    }

    // M88 nozzle, measured on the geo (bone frame, blocks, +Z = aft): exit plane 8.16 px behind the turbine pivot, inner radius ~5.9 px.
    private static final float NOZZLE_Z = 0.51F;
    private static final float NOZZLE_R = 0.36F;
    private static final int SEGMENTS = 16;
    private static final int RINGS = 12;

    /**
     * War Thunder style reheat, driven by the stage (0.2 = min reheat, 1 = full):
     * min reheat is a short, see-through blue-violet cone; full reheat is a long yellow-orange plume with a white core
     * and the nozzle opened wider. Nothing at all when unlit.
     */
    private void afterburner(VehicleModelInstance instance, String boneName, PoseStack poseStack, VertexConsumer vc,
                             float reheat, float speed, float time) {
        BoneState bone = instance.getBone(boneName);
        if (bone == null) {
            return;
        }
        poseStack.pushPose();
        // Same attachment as SBW muzzle flares: bone global transform, then its pivot = turbine centre.
        poseStack.mulPose(bone.getGlobalTransform(instance));
        var def = bone.definition();
        poseStack.translate(def.pivotX(), def.pivotY(), def.pivotZ());
        Matrix4f m = poseStack.last().pose();

        // w: 0 at min reheat (cool blue-violet), 1 at full reheat (hot yellow-orange). a: fade in while lighting up.
        float w = Mth.clamp((reheat - 0.2F) / 0.8F, 0F, 1F);
        float a = Mth.clamp(reheat / 0.2F, 0F, 1F) * (0.55F + 0.45F * w);

        disc(vc, m, NOZZLE_Z - 0.12F, NOZZLE_R,
                Mth.lerp(w, 0.8F, 1F), Mth.lerp(w, 0.75F, 0.95F), Mth.lerp(w, 1F, 0.8F), a,
                Mth.lerp(w, 0.6F, 1F), Mth.lerp(w, 0.35F, 0.55F), Mth.lerp(w, 0.9F, 0.2F), 0.6F * a);

        // Slow flicker so the plume breathes without strobing; full reheat is a bit more turbulent.
        float flicker = 1F - (0.04F + 0.04F * w) * (1F + Mth.sin(time * 1.9F) * 0.6F + Mth.sin(time * 5.3F) * 0.4F) * 0.5F;
        float k = (0.25F + 0.75F * reheat) * Mth.clamp(reheat / 0.2F, 0F, 1F) * speed * flicker;
        float z0 = NOZZLE_Z - 0.2F;
        float r = NOZZLE_R * (0.9F + 0.15F * w); // the nozzle petals open at full reheat
        plume(vc, m, z0, 3.4F * k, r, 1.25F * r, 0.3F * r,
                Mth.lerp(w, 0.45F, 1F), Mth.lerp(w, 0.3F, 0.55F), Mth.lerp(w, 0.9F, 0.25F),
                Mth.lerp(w, 0.35F, 0.55F), 0.2F, 0.6F, 0.28F * a);
        plume(vc, m, z0, 2.4F * k, 0.85F * r, r, 0.15F * r,
                Mth.lerp(w, 0.6F, 1F), Mth.lerp(w, 0.4F, 0.7F), Mth.lerp(w, 1F, 0.3F),
                Mth.lerp(w, 0.7F, 1F), Mth.lerp(w, 0.25F, 0.3F), Mth.lerp(w, 0.6F, 0.1F), 0.45F * a);
        plume(vc, m, z0, 1.1F * k, 0.7F * r, 0.75F * r, 0.1F * r,
                Mth.lerp(w, 0.85F, 1F), Mth.lerp(w, 0.8F, 0.95F), 1F - 0.2F * w,
                Mth.lerp(w, 0.7F, 1F), Mth.lerp(w, 0.5F, 0.75F), Mth.lerp(w, 1F, 0.4F), 0.7F * a);
        poseStack.popPose();
    }

    /** Tapered shell: swells to rPeak at 20% of its length, then closes; colour fades c0 -> c1, alpha -> 0. */
    private static void plume(VertexConsumer vc, Matrix4f m, float z0, float len, float rStart, float rPeak, float rEnd,
                              float r0, float g0, float b0, float r1, float g1, float b1, float alpha) {
        for (int i = 0; i < RINGS; i++) {
            float t0 = (float) i / RINGS;
            float t1 = (float) (i + 1) / RINGS;
            ring(vc, m,
                    z0 + len * t0, plumeRadius(t0, rStart, rPeak, rEnd), Mth.lerp(t0, r0, r1), Mth.lerp(t0, g0, g1), Mth.lerp(t0, b0, b1), alpha * (float) Math.pow(1F - t0, 1.6F),
                    z0 + len * t1, plumeRadius(t1, rStart, rPeak, rEnd), Mth.lerp(t1, r0, r1), Mth.lerp(t1, g0, g1), Mth.lerp(t1, b0, b1), alpha * (float) Math.pow(1F - t1, 1.6F));
        }
    }

    private static float plumeRadius(float t, float rStart, float rPeak, float rEnd) {
        return t < 0.2F ? Mth.lerp(t / 0.2F, rStart, rPeak) : Mth.lerp((float) Math.sqrt((t - 0.2F) / 0.8F), rPeak, rEnd);
    }

    private static void disc(VertexConsumer vc, Matrix4f m, float z, float radius,
                             float rc, float gc, float bc, float ac, float re, float ge, float be, float ae) {
        ring(vc, m, z, 0F, rc, gc, bc, ac, z, radius, re, ge, be, ae);
    }

    /** One band of quads around the Z axis between two rings. */
    private static void ring(VertexConsumer vc, Matrix4f m,
                             float za, float ra, float rA, float gA, float bA, float aA,
                             float zb, float rb, float rB, float gB, float bB, float aB) {
        for (int s = 0; s < SEGMENTS; s++) {
            float c0 = COS[s], s0 = SIN[s], c1 = COS[s + 1], s1 = SIN[s + 1];
            vc.addVertex(m, c0 * ra, s0 * ra, za).setColor(rA, gA, bA, aA);
            vc.addVertex(m, c1 * ra, s1 * ra, za).setColor(rA, gA, bA, aA);
            vc.addVertex(m, c1 * rb, s1 * rb, zb).setColor(rB, gB, bB, aB);
            vc.addVertex(m, c0 * rb, s0 * rb, zb).setColor(rB, gB, bB, aB);
        }
    }

    private static final float[] COS = new float[SEGMENTS + 1];
    private static final float[] SIN = new float[SEGMENTS + 1];

    static {
        for (int i = 0; i <= SEGMENTS; i++) {
            COS[i] = Mth.cos(i * Mth.TWO_PI / SEGMENTS);
            SIN[i] = Mth.sin(i * Mth.TWO_PI / SEGMENTS);
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
