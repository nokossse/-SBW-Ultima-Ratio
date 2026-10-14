package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

public class LeclercXlrRenderer extends GeoVehicleRenderer<VehicleEntity> {
    public LeclercXlrRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean hideForTurretControllerWhileZooming() {
        return true;
    }

    @Override
    public void transformCustomModelPart(
            VehicleEntity entity,
            VehicleModelInstance instance,
            PoseStack poseStack,
            float entityYaw,
            float partialTicks
    ) {
        super.transformCustomModelPart(entity, instance, poseStack, entityYaw, partialTicks);
        this.hideBone(instance, "hitboxes");
        this.hideBone(instance, "colission_hitbox");
        this.hideBone(instance, "hull_hitbox");
        this.hideBone(instance, "tracks_hitbox");
        this.hideBone(instance, "turret_hitbox");
        this.hideBone(instance, "driver_camera_hitbox");
        BoneState rws = instance.getBone("passengerWeaponStation");
        if (rws != null) {
            rws.visible = true;
        }
    }

    protected void hideBone(VehicleModelInstance instance, String name) {
        BoneState bone = instance.getBone(name);
        if (bone == null) {
            return;
        }
        bone.visible = false;
        bone.x = 0.0F;
        bone.y = -4096.0F;
        bone.z = 0.0F;
    }

    @Override
    public float getTrackDistance() {
        return 2.27F;
    }

    @Override
    public float getBoneRotX(float t) {
        if (t <= 41.25F) {
            return 0.0F;
        }
        if (t <= 47.25F) {
            return Mth.lerp((t - 41.25F) / (47.25F - 41.25F), 0.0F, -147.5F);
        }
        if (t <= 53.9167F) {
            return -147.5F;
        }
        if (t <= 54.5833F) {
            return Mth.lerp((t - 53.9167F) / (54.5833F - 53.9167F), -147.5F, -180.0F);
        }
        if (t <= 85.5F) {
            return -180.0F;
        }
        if (t <= 85.9167F) {
            return -180.0F;
        }
        if (t <= 86.4167F) {
            return Mth.lerp((t - 85.9167F) / (86.4167F - 85.9167F), -180.0F, -205.0F);
        }
        if (t <= 93.6667F) {
            return -205.0F;
        }
        if (t <= 100.0F) {
            return Mth.lerp((t - 93.6667F) / (99.5F - 93.6667F), -205.0F, -360.0F);
        }
        return 0.0F;
    }

    @Override
    public float getBoneMoveY(float t) {
        if (t <= 41.75F) {
            return 0.0F;
        }
        if (t <= 42.5833F) {
            return Mth.lerp((t - 41.75F) / (42.5833F - 41.75F), 0.0F, -0.72F);
        }
        if (t <= 43.5F) {
            return Mth.lerp((t - 42.5833F) / (43.5F - 42.5833F), -0.72F, -2.175F);
        }
        if (t <= 44.3333F) {
            return Mth.lerp((t - 43.5F) / (44.3333F - 43.5F), -2.175F, -5.01F);
        }
        if (t <= 45.25F) {
            return Mth.lerp((t - 44.3333F) / (45.25F - 44.3333F), -5.01F, -7.8F);
        }
        if (t <= 46.0833F) {
            return Mth.lerp((t - 45.25F) / (46.0833F - 45.25F), -7.8F, -10.6F);
        }
        if (t <= 46.9167F) {
            return Mth.lerp((t - 46.0833F) / (46.9167F - 46.0833F), -10.6F, -13.245F);
        }
        if (t <= 47.8333F) {
            return Mth.lerp((t - 46.9167F) / (47.8333F - 46.9167F), -13.245F, -14.81F);
        }
        if (t <= 53.5833F) {
            return Mth.lerp((t - 47.8333F) / (53.5833F - 47.8333F), -14.81F, -24.64F);
        }
        if (t <= 54.25F) {
            return Mth.lerp((t - 53.5833F) / (54.25F - 53.5833F), -24.64F, -25.39F);
        }
        if (t <= 54.9167F) {
            return Mth.lerp((t - 54.25F) / (54.9167F - 54.25F), -25.39F, -25.74F);
        }
        if (t <= 84.9167F) {
            return Mth.lerp((t - 54.9167F) / (84.9167F - 54.9167F), -25.74F, -25.73F);
        }
        if (t <= 85.6667F) {
            return Mth.lerp((t - 84.9167F) / (85.6667F - 84.9167F), -25.73F, -25.33F);
        }
        if (t <= 86.4167F) {
            return Mth.lerp((t - 85.6667F) / (86.4167F - 85.6667F), -25.33F, -24.72F);
        }
        if (t <= 93.1667F) {
            return Mth.lerp((t - 86.4167F) / (93.1667F - 86.4167F), -24.72F, -15.75F);
        }
        if (t <= 93.9167F) {
            return Mth.lerp((t - 93.1667F) / (93.9167F - 93.1667F), -15.75F, -14.53F);
        }
        if (t <= 94.5833F) {
            return Mth.lerp((t - 93.9167F) / (94.5833F - 93.9167F), -14.53F, -12.57F);
        }
        if (t <= 95.25F) {
            return Mth.lerp((t - 94.5833F) / (95.25F - 94.5833F), -12.57F, -10.62F);
        }
        if (t <= 95.9167F) {
            return Mth.lerp((t - 95.25F) / (95.9167F - 95.25F), -10.62F, -8.48F);
        }
        if (t <= 96.6667F) {
            return Mth.lerp((t - 95.9167F) / (96.6667F - 95.9167F), -8.48F, -6.2F);
        }
        if (t <= 97.25F) {
            return Mth.lerp((t - 96.6667F) / (97.25F - 96.6667F), -6.2F, -4.35F);
        }
        if (t <= 97.8333F) {
            return Mth.lerp((t - 97.25F) / (97.8333F - 97.25F), -4.35F, -2.68F);
        }
        if (t <= 98.5F) {
            return Mth.lerp((t - 97.8333F) / (98.5F - 97.8333F), -2.68F, -1.26F);
        }
        if (t <= 98.8333F) {
            return Mth.lerp((t - 98.5F) / (98.8333F - 98.5F), -1.26F, -0.68F);
        }
        if (t <= 99.25F) {
            return Mth.lerp((t - 98.8333F) / (99.25F - 98.8333F), -0.68F, -0.3F);
        }
        if (t <= 99.5833F) {
            return Mth.lerp((t - 99.25F) / (99.5833F - 99.25F), -0.3F, 0.0F);
        }
        return 0.0F;
    }

    @Override
    public float getBoneMoveZ(float t) {
        if (t <= 41.75F) {
            return Mth.lerp(t / 41.75F, 0.0F, 126.5F);
        }
        if (t <= 42.5833F) {
            return Mth.lerp((t - 41.75F) / (42.5833F - 41.75F), 126.5F, 129.37F);
        }
        if (t <= 43.5F) {
            return Mth.lerp((t - 42.5833F) / (43.5F - 42.5833F), 129.37F, 131.74F);
        }
        if (t <= 44.3333F) {
            return Mth.lerp((t - 43.5F) / (44.3333F - 43.5F), 131.74F, 133.605F);
        }
        if (t <= 45.25F) {
            return Mth.lerp((t - 44.3333F) / (45.25F - 44.3333F), 133.605F, 134.085F);
        }
        if (t <= 46.0833F) {
            return Mth.lerp((t - 45.25F) / (46.0833F - 45.25F), 134.085F, 133.565F);
        }
        if (t <= 46.9167F) {
            return Mth.lerp((t - 46.0833F) / (46.9167F - 46.0833F), 133.565F, 131.6F);
        }
        if (t <= 47.8333F) {
            return Mth.lerp((t - 46.9167F) / (47.8333F - 46.9167F), 131.6F, 129.55F);
        }
        if (t <= 53.5833F) {
            return Mth.lerp((t - 47.8333F) / (53.5833F - 47.8333F), 129.55F, 114.11F);
        }
        if (t <= 54.25F) {
            return Mth.lerp((t - 53.5833F) / (54.25F - 53.5833F), 114.11F, 112.38F);
        }
        if (t <= 54.9167F) {
            return Mth.lerp((t - 54.25F) / (54.9167F - 54.25F), 112.38F, 110.33F);
        }
        if (t <= 84.9167F) {
            return Mth.lerp((t - 54.9167F) / (84.9167F - 54.9167F), 110.33F, 16.99F);
        }
        if (t <= 85.6667F) {
            return Mth.lerp((t - 84.9167F) / (85.6667F - 84.9167F), 16.99F, 14.71F);
        }
        if (t <= 86.4167F) {
            return Mth.lerp((t - 85.6667F) / (86.4167F - 85.6667F), 14.71F, 12.51F);
        }
        if (t <= 93.1667F) {
            return Mth.lerp((t - 86.4167F) / (93.1667F - 86.4167F), 12.51F, -6.97F);
        }
        if (t <= 93.9167F) {
            return Mth.lerp((t - 93.1667F) / (93.9167F - 93.1667F), -6.97F, -8.64F);
        }
        if (t <= 94.5833F) {
            return Mth.lerp((t - 93.9167F) / (94.5833F - 93.9167F), -8.64F, -10.07F);
        }
        if (t <= 95.25F) {
            return Mth.lerp((t - 94.5833F) / (95.25F - 94.5833F), -10.07F, -10.86F);
        }
        if (t <= 95.9167F) {
            return Mth.lerp((t - 95.25F) / (95.9167F - 95.25F), -10.86F, -11.18F);
        }
        if (t <= 96.6667F) {
            return Mth.lerp((t - 95.9167F) / (96.6667F - 95.9167F), -11.18F, -10.8F);
        }
        if (t <= 97.25F) {
            return Mth.lerp((t - 96.6667F) / (97.25F - 96.6667F), -10.8F, -10.05F);
        }
        if (t <= 97.8333F) {
            return Mth.lerp((t - 97.25F) / (97.8333F - 97.25F), -10.05F, -8.695F);
        }
        if (t <= 98.5F) {
            return Mth.lerp((t - 97.8333F) / (98.5F - 97.8333F), -8.695F, -6.38F);
        }
        if (t <= 98.8333F) {
            return Mth.lerp((t - 98.5F) / (98.8333F - 98.5F), -6.38F, -5.01F);
        }
        if (t <= 99.25F) {
            return Mth.lerp((t - 98.8333F) / (99.25F - 98.8333F), -5.01F, -3.39F);
        }
        if (t <= 99.5833F) {
            return Mth.lerp((t - 99.25F) / (99.5833F - 99.25F), -3.39F, -1.69F);
        }
        return Mth.lerp((t - 99.5833F) / (100.0F - 99.5833F), -1.69F, 0.0F);
    }
}
