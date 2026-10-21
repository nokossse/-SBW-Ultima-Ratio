package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationContext;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Rafale C: SBW aircraft flight model (like the A-10), only the landing gear clips are ours. */
public class RafaleEntity extends VehicleEntity {
    private static final String GEAR_UP_ANIM = "animation.rafale.gear_up";
    private static final String GEAR_DOWN_ANIM = "animation.rafale.gear_down";

    /** Client-only turbine spin angle in degrees; spins faster the faster the jet flies. */
    public float reactorSpin;
    public float reactorSpinO;
    public static final double DRY_MAX_KMH = 270;
    /** The violet cone starts to show here and reaches its first full size at DRY_MAX_KMH. */
    public static final double FADE_IN_KMH = 200;

    /** Client-only reheat stage 0..1 (0.2-0.55 partial from speed, 1 = full boost), smoothed from synced data. */
    public float afterburner;
    public float afterburnerO;

    public RafaleEntity(EntityType<? extends RafaleEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (!this.level().isClientSide) {
            return;
        }

        this.reactorSpinO = this.reactorSpin;
        this.reactorSpin = (this.reactorSpin + 10F + 60F * (float) this.getDeltaMovement().length()) % 360F;

        // Reheat stage 0..1, only while accelerating (forward key). Forward + sprint = full reheat at any speed.
        // Otherwise a violet cone fading in from FADE_IN_KMH, then partial reheat past the dry top speed, growing with speed (absoluteSpeed is blocks/tick * 72 = km/h).
        double kmh = this.getAbsoluteSpeed() * 72;
        boolean throttle = this.forwardInputDown();
        float target = !throttle ? 0F
                : this.sprintInputDown() ? 1F
                : kmh <= DRY_MAX_KMH ? 0.2F * (float) Mth.clamp((kmh - FADE_IN_KMH) / (DRY_MAX_KMH - FADE_IN_KMH), 0, 1)
                : 0.2F + 0.35F * (float) Mth.clamp((kmh - DRY_MAX_KMH) / 330.0, 0, 1);
        this.afterburnerO = this.afterburner;
        this.afterburner = Mth.approach(this.afterburner, target, target > this.afterburner ? 0.25F : 0.08F);

        float gearRot = this.getSynchedGearRot();
        boolean up = this.getGearUp();
        boolean gearUp = (up && gearRot > 0 && gearRot < 1) || gearRot == 1f;
        boolean gearDown = (!up && gearRot > 0 && gearRot < 1) || gearRot == 0f;

        VehicleAnimationInstance<?> anim = this.getAnim();
        VehicleAnimationContext<?> ctx = anim == null ? null : anim.getContext();
        if (ctx == null) {
            return;
        }
        if (gearUp && !this.getWasGearUp()) {
            ctx.stopAnimation(GEAR_DOWN_ANIM, 0);
            ctx.playAnimation(GEAR_UP_ANIM, AnimationPlayType.PLAY_ONCE_HOLD, 0);
            this.setWasGearUp(true);
        } else if (gearDown && this.getWasGearUp()) {
            ctx.stopAnimation(GEAR_UP_ANIM, 0);
            ctx.playAnimation(GEAR_DOWN_ANIM, AnimationPlayType.PLAY_ONCE_HOLD, 0);
            this.setWasGearUp(false);
        }
    }
}
