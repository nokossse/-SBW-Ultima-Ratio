package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationContext;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.SpArtilleryEntity;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class CaesarEntity extends SpArtilleryEntity {
    public static final EntityDataAccessor<Boolean> STABILIZED =
            SynchedEntityData.defineId(CaesarEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> STABILIZING =
            SynchedEntityData.defineId(CaesarEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> PACKING =
            SynchedEntityData.defineId(CaesarEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> STAB_BLEND =
            SynchedEntityData.defineId(CaesarEntity.class, EntityDataSerializers.FLOAT);

    private static final String STAB_ANIM = "stabilization";
    private static final String PACK_ANIM = "stabilization_pack";
    private static final String RELOAD_ANIM = "animation.main.reload";
    private static final int STAB_DURATION_TICKS = 60;
    private static final double AIM_START_DEGREES = 2.0;
    private static final float TURRET_HOME_SPEED = 1.5F;

    private int stabilizeTicks;
    private int packTicks;
    private boolean wasStabilizing;
    private boolean wasStabilized;
    private boolean wasPacking;
    private boolean wasReloading;

    public CaesarEntity(EntityType<? extends CaesarEntity> type, Level level) {
        super(type, level);
    }

    public boolean isStabilized() {
        return this.entityData.get(STABILIZED);
    }

    public void setStabilized(boolean value) {
        this.entityData.set(STABILIZED, value);
    }

    public boolean isStabilizing() {
        return this.entityData.get(STABILIZING);
    }

    public void setStabilizing(boolean value) {
        this.entityData.set(STABILIZING, value);
    }

    public boolean isPacking() {
        return this.entityData.get(PACKING);
    }

    public void setPacking(boolean value) {
        this.entityData.set(PACKING, value);
    }

    public float getStabBlend() {
        return this.entityData.get(STAB_BLEND);
    }

    public boolean blocksDriving() {
        return this.isStabilized() || this.isStabilizing() || this.isPacking();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STABILIZED, false);
        this.entityData.define(STABILIZING, false);
        this.entityData.define(PACKING, false);
        this.entityData.define(STAB_BLEND, 0.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Stabilized", this.isStabilized());
        compound.putBoolean("Stabilizing", this.isStabilizing());
        compound.putBoolean("Packing", this.isPacking());
        compound.putInt("StabilizeTicks", this.stabilizeTicks);
        compound.putInt("PackTicks", this.packTicks);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Stabilized")) {
            this.setStabilized(compound.getBoolean("Stabilized"));
        }
        if (compound.contains("Stabilizing")) {
            this.setStabilizing(compound.getBoolean("Stabilizing"));
        }
        if (compound.contains("Packing")) {
            this.setPacking(compound.getBoolean("Packing"));
        }
        if (compound.contains("StabilizeTicks")) {
            this.stabilizeTicks = compound.getInt("StabilizeTicks");
        }
        if (compound.contains("PackTicks")) {
            this.packTicks = compound.getInt("PackTicks");
        }
    }

    @Override
    public void processInput(short input) {
        boolean wantsToDrive = (input & 4) != 0 || (input & 8) != 0;
        super.processInput(input);
        if (!this.blocksDriving()) {
            return;
        }
        if (wantsToDrive) {
            this.requestPackUp();
        }
        this.setForwardInputDown(false);
        this.setBackInputDown(false);
    }

    @Override
    public void travel() {
        if (this.blocksDriving()) {
            this.setForwardInputDown(false);
            this.setBackInputDown(false);
            this.setTargetSpeed(0.0);
        }
        super.travel();
    }

    @Override
    public void baseTick() {
        super.baseTick();
        this.tickStabilization();
        this.updateStabBlend();
        if (this.level().isClientSide) {
            this.tickStabilizationAnimation();
            this.tickReloadAnimation();
        }
    }

    private void tickStabilization() {
        Entity gunner = this.getNthEntity(this.getTurretControllerIndex());

        if (this.isPacking()) {
            this.setLockTurret(true);
            this.homeTurret();
            if (!this.level().isClientSide) {
                this.packTicks--;
                if (this.packTicks <= 0) {
                    this.finishPackUp();
                }
            }
            return;
        }

        if (this.isStabilizing()) {
            this.setLockTurret(true);
            if (!this.level().isClientSide) {
                this.stabilizeTicks--;
                if (this.stabilizeTicks <= 0) {
                    this.setStabilizing(false);
                    this.setStabilized(true);
                    this.setLockTurret(false);
                }
            }
            return;
        }

        if (this.isStabilized()) {
            this.setLockTurret(gunner == null);
            return;
        }

        this.setLockTurret(true);
        this.homeTurret();
        if (!this.level().isClientSide && gunner != null && this.gunnerWantsToAim(gunner)) {
            this.startStabilization();
        }
    }

    private void updateStabBlend() {
        if (this.level().isClientSide) {
            return;
        }
        float blend;
        if (this.isPacking()) {
            blend = this.packTicks / (float) STAB_DURATION_TICKS;
        } else if (this.isStabilizing()) {
            blend = 1.0F - this.stabilizeTicks / (float) STAB_DURATION_TICKS;
        } else if (this.isStabilized()) {
            blend = 1.0F;
        } else {
            blend = 0.0F;
        }
        this.entityData.set(STAB_BLEND, blend);
    }

    /** Slew barrel/yaw back to travel rest (0°) like the FH77 when packing up. */
    private void homeTurret() {
        this.setTurretXRot(Mth.approach(this.getTurretXRot(), 0.0F, TURRET_HOME_SPEED));
        this.setTurretYRot(Mth.approach(this.getTurretYRot(), 0.0F, TURRET_HOME_SPEED));
    }

    private boolean gunnerWantsToAim(Entity gunner) {
        Vec3 wanted = gunner.getViewVector(1.0F);
        Vec3 barrel = this.getShootVec("Main", 1.0F);
        return VectorTool.calculateAngle(wanted, barrel) > AIM_START_DEGREES;
    }

    private void startStabilization() {
        this.setPacking(false);
        this.packTicks = 0;
        this.setStabilizing(true);
        this.setStabilized(false);
        this.stabilizeTicks = STAB_DURATION_TICKS;
        this.setLockTurret(true);
    }

    private void requestPackUp() {
        if (this.level().isClientSide || this.isPacking()) {
            return;
        }
        if (!this.isStabilized() && !this.isStabilizing()) {
            return;
        }
        this.setStabilizing(false);
        this.setStabilized(false);
        this.stabilizeTicks = 0;
        this.setPacking(true);
        this.packTicks = STAB_DURATION_TICKS;
        this.setLockTurret(true);
    }

    private void finishPackUp() {
        this.setPacking(false);
        this.setStabilizing(false);
        this.setStabilized(false);
        this.packTicks = 0;
        this.stabilizeTicks = 0;
        this.setTurretXRot(0.0F);
        this.setTurretYRot(0.0F);
        this.setLockTurret(true);
    }

    private void tickStabilizationAnimation() {
        VehicleAnimationInstance<?> anim = this.getAnim();
        if (anim == null) {
            return;
        }
        VehicleAnimationContext<?> context = anim.getContext();
        if (context == null) {
            return;
        }

        boolean packing = this.isPacking();
        boolean stabilizing = this.isStabilizing();
        boolean stabilized = this.isStabilized();

        if (packing && !this.wasPacking) {
            context.stopAnimation(STAB_ANIM, 0);
            context.playAnimation(PACK_ANIM, AnimationPlayType.PLAY_ONCE_HOLD, 2);
        } else if (stabilizing && !this.wasStabilizing) {
            context.stopAnimation(PACK_ANIM, 0);
            context.playAnimation(STAB_ANIM, AnimationPlayType.PLAY_ONCE_HOLD, 5);
        } else if (stabilized && !this.wasStabilized && !this.wasStabilizing) {
            context.stopAnimation(PACK_ANIM, 0);
            context.playAnimation(STAB_ANIM, AnimationPlayType.PLAY_ONCE_HOLD, 0);
        } else if (!packing && !stabilizing && !stabilized && (this.wasPacking || this.wasStabilizing || this.wasStabilized)) {
            context.stopAnimation(STAB_ANIM, 8);
            context.stopAnimation(PACK_ANIM, 8);
        }

        this.wasPacking = packing;
        this.wasStabilizing = stabilizing;
        this.wasStabilized = stabilized;
    }

    private void tickReloadAnimation() {
        GunData gun = this.getGunData("Main");
        boolean reloading = gun != null && gun.reloading();
        if (reloading == this.wasReloading) {
            return;
        }

        VehicleAnimationInstance<?> anim = this.getAnim();
        if (anim == null) {
            return;
        }
        VehicleAnimationContext<?> context = anim.getContext();
        if (context == null) {
            return;
        }

        if (reloading) {
            context.playAnimation(RELOAD_ANIM, AnimationPlayType.PLAY_ONCE_STOP, 2);
        } else {
            context.stopAnimation(RELOAD_ANIM, 5);
        }
        this.wasReloading = reloading;
    }

    @Override
    public boolean canBind() {
        return true;
    }

    @Override
    public float getTurretTurnXSpeed() {
        return this.getLockTurret() ? 0.0F : super.getTurretTurnXSpeed();
    }

    @Override
    public float getTurretTurnYSpeed() {
        return this.getLockTurret() ? 0.0F : super.getTurretTurnYSpeed();
    }
}
