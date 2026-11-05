package com.nokosse.ultimaratio.client.renderer;

import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance;
import com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * The wheeled trucks of the addon (Kerax family, Thundart): every wheel spins, the front axle (wheelL0Turn /
 * wheelR0Turn) steers a little more than SBW's rudder angle and the steering wheel ("volant") turns with it.
 */
public abstract class TruckRenderer<T extends VehicleEntity> extends GeoVehicleRenderer<T> {
    /** SBW caps the rudder at ~0.6 rad (~34 degrees): a little extra lock on the front wheels. */
    private static final float STEER_LOCK_MULT = 1.35F;
    private static final float VOLANT_TURNS = 12.0F;
    private static final float WHEEL_SPIN = 1.5F;

    protected TruckRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean hideForTurretControllerWhileZooming() {
        return true;
    }

    /**
     * @param spinSign   1 or -1: which way the model's wheels roll forward
     * @param volantSign 1 or -1: which way the model's steering wheel turns to the right
     */
    protected void driveTruck(T entity, VehicleModelInstance instance, float partialTicks, float spinSign, float volantSign) {
        float steer = Mth.lerp(partialTicks, entity.getRudderRotO(), entity.getRudderRot());
        float leftSpin = spinSign * this.getLeftWheelRot();
        float rightSpin = spinSign * this.getRightWheelRot();
        for (BoneState bone : instance.getBoneGroups().getLeftWheels()) {
            if (bone != null && bone.rotation != null) {
                bone.rotation.rotationX(WHEEL_SPIN * leftSpin);
            }
        }
        for (BoneState bone : instance.getBoneGroups().getRightWheels()) {
            if (bone != null && bone.rotation != null) {
                bone.rotation.rotationX(WHEEL_SPIN * rightSpin);
            }
        }
        float lock = steer * STEER_LOCK_MULT;
        steerFront(instance.getBone("wheelL0Turn"), lock, leftSpin);
        steerFront(instance.getBone("wheelR0Turn"), lock, rightSpin);

        BoneState volant = instance.getBone("volant");
        if (volant != null && volant.rotation != null) {
            volant.rotation.set(volant.definition().bindRotation());
            volant.rotation.rotateZ(volantSign * VOLANT_TURNS * steer);
        }
    }

    private static void steerFront(BoneState bone, float steer, float spin) {
        if (bone == null || bone.rotation == null) {
            return;
        }
        bone.rotation.set(bone.definition().bindRotation());
        bone.rotation.rotateY(steer);
        bone.rotation.rotateX(WHEEL_SPIN * spin);
    }

    /** Hitbox and helper bones left in the model: hidden and moved out of the way. */
    protected static void hideBones(VehicleModelInstance instance, String... names) {
        for (String name : names) {
            BoneState bone = instance.getBone(name);
            if (bone != null) {
                bone.visible = false;
                bone.x = 0.0F;
                bone.y = -4096.0F;
                bone.z = 0.0F;
            }
        }
    }
}
