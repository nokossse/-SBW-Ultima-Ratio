package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationContext;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance;
import com.nokosse.ultimaratio.inventory.Mq9Menu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.List;

/**
 * General Atomics MQ-9 Reaper: the VSR700's remote pilot (VSR Monitor, see Vsr700Entity) on SBW's aircraft flight
 * model. Released by its operator in the air it circles around that point with SBW's own loiter; the landing gear
 * clips are played like the Rafale's.
 */
public class Mq9ReaperEntity extends Vsr700Entity {
    private static final String GEAR_UP_ANIM = "animation.reaper.gear-up";
    private static final String GEAR_DOWN_ANIM = "animation.reaper.gear-down";

    /** True during tick(): see EntityPassengersMixin. */
    private boolean passengerStandIn;
    @Nullable
    private Marker standIn;

    public Mq9ReaperEntity(EntityType<? extends Mq9ReaperEntity> type, Level level) {
        super(type, level);
    }

    public boolean isPassengerStandIn() {
        return passengerStandIn;
    }

    /**
     * SBW's aircraft engine drops the controls of a plane with no passenger and its loiter only runs with one, but the
     * operator is no passenger: while the plane ticks, getPassengers (final, mixed in) returns this unseen marker
     * (not a LivingEntity, so a crash hurts nobody).
     */
    public List<Entity> standInPassengers() {
        if (standIn == null) {
            standIn = new Marker(EntityType.MARKER, level());
        }
        return List.of(standIn);
    }

    @Override
    public void tick() {
        passengerStandIn = true;
        try {
            super.tick();
        } finally {
            passengerStandIn = false;
        }
        Vec3 motion = getDeltaMovement();
        // Help it down when the loiter wants a lower altitude than it holds
        double above = getY() - getLoiterCenterY();
        if (getLoiterActive() && !onGround() && above > 1.0) {
            motion = motion.add(0, -Math.min(0.05, above * 0.003), 0);
        }
        // 130 km/h, 160 with the sprint key (full throttle); eased down when it no longer applies
        double limit = (sprintInputDown() ? 160.0 : 130.0) / 72.0; // 20 m/s = 1 block/tick = 72 km/h
        if (motion.lengthSqr() > limit * limit) {
            motion = motion.scale(Math.max(limit / motion.length(), 0.97));
        }
        setDeltaMovement(motion);
    }

    @Override
    protected Vec3 lensPivot() {
        return new Vec3(0, 1.557, 4.843);
    }

    @Override
    protected double lensReach() {
        return 0.44;
    }

    @Override
    public double maxRange() {
        return 5000.0;
    }

    @Override
    public double jamRange() {
        return 4500.0;
    }

    @Override
    public double warnRange() {
        return 4800.0;
    }

    /** Hands off in the air: keep the loiter that was set, else circle around here at SBW's loiter radius. */
    @Override
    public void setOperator(@Nullable ServerPlayer operator) {
        super.setOperator(operator);
        if (operator == null && !onGround() && !isWreck() && !getLoiterActive()) {
            setLoiterParams(new Quaternionf((float) getX(), (float) getY(), (float) getZ(), (float) getLoiterRadius()));
            setLoiterActive(true);
        }
    }

    @Override
    protected AbstractContainerMenu newMenu(int id, Inventory inventory) {
        return new Mq9Menu(id, inventory, getId());
    }

    @Override
    protected int magazineSlots() {
        return 4;
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (!this.level().isClientSide) {
            return;
        }

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
