package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.mojang.blaze3d.vertex.PoseStack;
import com.nokosse.ultimaratio.entity.RadarVehicleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;

/** The SAMP/T radar trucks (ARABEL, NG): a Kerax whose antenna bone turns once the radar is deployed. */
public class RadarTruckRenderer extends TruckRenderer<RadarVehicleEntity> {
    private final String antennaBone;

    public RadarTruckRenderer(EntityRendererProvider.Context context, String antennaBone) {
        super(context);
        this.antennaBone = antennaBone;
    }

    /** ARABEL's rotating antenna. */
    public static RadarTruckRenderer arabel(EntityRendererProvider.Context context) {
        return new RadarTruckRenderer(context, "arabel_rot");
    }

    /** Ground Fire 300 antenna of the SAMP/T NG. */
    public static RadarTruckRenderer groundFire(EntityRendererProvider.Context context) {
        return new RadarTruckRenderer(context, "rolling");
    }

    @Override
    public void transformCustomModelPart(RadarVehicleEntity entity, VehicleModelInstance instance, PoseStack poseStack,
                                         float entityYaw, float partialTicks) {
        super.transformCustomModelPart(entity, instance, poseStack, entityYaw, partialTicks);
        hideBones(instance, "hitboxes", "collision", "dmg");
        this.driveTruck(entity, instance, partialTicks, -1F, -1F);

        BoneState dish = instance.getBone(this.antennaBone);
        if (dish != null && dish.rotation != null) {
            dish.rotation.set(dish.definition().bindRotation());
            if (entity.isRadarDishSpinning()) {
                dish.rotation.rotateY(entity.getRadarDishYawRad(partialTicks));
            }
        }
    }
}
