package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationContext;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.SpArtilleryEntity;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class VbmrMepacEntity extends SpArtilleryEntity {
    public static final float SEAT_Y = -0.5F;
    public static final int MACHINE_GUN_SEAT = 1;

    public static final EntityDataAccessor<Boolean> CLOSE =
            SynchedEntityData.defineId(VbmrMepacEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> OPENED =
            SynchedEntityData.defineId(VbmrMepacEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> OUT =
            SynchedEntityData.defineId(VbmrMepacEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Float> MG_YAW =
            SynchedEntityData.defineId(VbmrMepacEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> MG_PITCH =
            SynchedEntityData.defineId(VbmrMepacEntity.class, EntityDataSerializers.FLOAT);

    private static final String TRAP_OPEN_ANIM = "vmbr-mortar-trap-open";
    private static final String TRAP_CLOSE_ANIM = "vmbr-mortar-trap-close";
    private static final String DEPLOY_ANIM = "vmbr-mortar-deploy";
    private static final String SHOOT_ANIM = "vbmr-mortar-shoot";
    private static final String RELOAD_ANIM = "vbmr-mortar-reload";
    private static final int SHOOT_ANIM_TICKS = 20;
    private static final float AIM_MIN_PITCH = 45.0F;

    private boolean wasClose = true;
    private boolean wasOut;
    private boolean wasReloading;
    private int clientShootTicks;

    public VbmrMepacEntity(EntityType<? extends VbmrMepacEntity> type, Level level) {
        super(type, level);
    }

    public boolean getClose() {
        return this.entityData.get(CLOSE);
    }

    public void setClose(boolean value) {
        this.entityData.set(CLOSE, value);
    }

    public boolean getOpened() {
        return this.entityData.get(OPENED);
    }

    public void setOpened(boolean value) {
        this.entityData.set(OPENED, value);
    }

    public boolean getOut() {
        return this.entityData.get(OUT);
    }

    public void setOut(boolean value) {
        this.entityData.set(OUT, value);
    }

    public float getMgYaw() {
        return this.entityData.get(MG_YAW);
    }

    public void setMgYaw(float value) {
        this.entityData.set(MG_YAW, value);
    }

    public float getMgPitch() {
        return this.entityData.get(MG_PITCH);
    }

    public void setMgPitch(float value) {
        this.entityData.set(MG_PITCH, value);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CLOSE, true);
        this.entityData.define(OPENED, false);
        this.entityData.define(OUT, false);
        this.entityData.define(MG_YAW, 0.0F);
        this.entityData.define(MG_PITCH, 0.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Close", this.getClose());
        compound.putBoolean("Opened", this.getOpened());
        compound.putBoolean("Out", this.getOut());
        compound.putFloat("MgYaw", this.getMgYaw());
        compound.putFloat("MgPitch", this.getMgPitch());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Close")) {
            this.setClose(compound.getBoolean("Close"));
        }
        if (compound.contains("Opened")) {
            this.setOpened(compound.getBoolean("Opened"));
        }
        if (compound.contains("Out")) {
            this.setOut(compound.getBoolean("Out"));
        }
        if (compound.contains("MgYaw")) {
            this.setMgYaw(compound.getFloat("MgYaw"));
        }
        if (compound.contains("MgPitch")) {
            this.setMgPitch(compound.getFloat("MgPitch"));
        }
    }

    @Override
    public void passengerPos(Entity passenger, Entity.MoveFunction callback, Vec3 vec3, String transform) {
        super.passengerPos(passenger, callback, vec3.add(0.0, SEAT_Y, 0.0), transform);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        this.captureMachineGunPose();
        boolean tryMove = this.forwardInputDown() || this.backInputDown();
        Entity gunner = this.getNthEntity(this.getTurretControllerIndex());
        if (gunner == null) {
            if (this.getDeltaMovement().horizontalDistanceSqr() > 0.007 || tryMove) {
                this.setTurretXRot(Mth.approach(this.getTurretXRot(), 0.0F, 2.5F));
                this.setTurretYRot(Mth.approach(this.getTurretYRot(), 0.0F, 2.5F));
                this.setOut(false);
                this.setLockTurret(true);
                if (Mth.abs(this.getTurretXRot()) < 0.5F && Mth.abs(this.getTurretYRot()) < 0.5F) {
                    this.setClose(true);
                }
            } else if (!this.getLockTurret()) {
                this.setClose(false);
            }
            this.setOpened(false);
        } else {
            this.setLockTurret(false);
            this.setClose(false);
            if (this.getOpened()) {
                this.setOut(true);
            }
        }
        if (gunner != null && VectorTool.calculateAngle(gunner.getLookAngle(), this.getShootVec("Main", 1.0F)) > 25.0) {
            this.setOpened(true);
        }
        if (this.level().isClientSide) {
            this.tickCoverAnimation();
            this.tickFireAnimation();
        }
        if (!this.getLockTurret()) {
            this.setPower(0.0F);
            if (this.getTurretXRot() > -AIM_MIN_PITCH) {
                this.setTurretXRot(-AIM_MIN_PITCH);
            }
        }
    }

    private void captureMachineGunPose() {
        Entity gunner = this.getNthEntity(MACHINE_GUN_SEAT);
        if (gunner == null) {
            return;
        }
        Vec3 defaultVec = this.getDefaultBarrelDirection(MACHINE_GUN_SEAT, 1.0F);
        Vec3 targetVec = this.getShootVec(MACHINE_GUN_SEAT, 1.0F);
        if (defaultVec == null || targetVec == null) {
            return;
        }
        float diffY = (float) Mth.wrapDegrees(
                -VehicleVecUtils.getYRotFromVector(targetVec) + VehicleVecUtils.getYRotFromVector(defaultVec));
        float diffX = (float) Mth.wrapDegrees(
                -VehicleVecUtils.getXRotFromVector(targetVec) + VehicleVecUtils.getXRotFromVector(defaultVec));
        this.setMgYaw(-diffY);
        this.setMgPitch(-diffX);
    }

    private void tickCoverAnimation() {
        VehicleAnimationContext<?> context = this.animContext();
        if (context == null) {
            return;
        }
        if (this.getClose() && !this.wasClose) {
            context.stopAnimation(TRAP_OPEN_ANIM, 8);
            context.playAnimation(TRAP_CLOSE_ANIM, AnimationPlayType.PLAY_ONCE_HOLD, 8);
        } else if (!this.getClose() && this.wasClose) {
            context.stopAnimation(TRAP_CLOSE_ANIM, 8);
            context.playAnimation(TRAP_OPEN_ANIM, AnimationPlayType.PLAY_ONCE_HOLD, 8);
        }
        if (this.getOut() && !this.wasOut) {
            context.playAnimation(DEPLOY_ANIM, AnimationPlayType.PLAY_ONCE_HOLD, 8);
        } else if (!this.getOut() && this.wasOut) {
            context.stopAnimation(DEPLOY_ANIM, 8);
        }
        this.wasClose = this.getClose();
        this.wasOut = this.getOut();
    }

    private void tickFireAnimation() {
        GunData gun = this.getGunData("Main");
        boolean reloading = gun != null && gun.reloading();
        VehicleAnimationContext<?> context = this.animContext();
        if (context == null) {
            this.wasReloading = reloading;
            return;
        }
        if (reloading && !this.wasReloading) {
            context.playAnimation(SHOOT_ANIM, AnimationPlayType.PLAY_ONCE_STOP, 1);
            this.clientShootTicks = SHOOT_ANIM_TICKS;
        }
        if (this.clientShootTicks > 0) {
            this.clientShootTicks--;
            if (this.clientShootTicks == 0 && reloading) {
                context.playAnimation(RELOAD_ANIM, AnimationPlayType.PLAY_ONCE_STOP, 2);
            }
        }
        this.wasReloading = reloading;
    }

    private VehicleAnimationContext<?> animContext() {
        VehicleAnimationInstance<?> anim = this.getAnim();
        if (anim == null) {
            return null;
        }
        return anim.getContext();
    }

    private boolean mortarBlockedByMovement() {
        return this.getDeltaMovement().lengthSqr() > 0.001;
    }

    private void warnNotStopped(@Nullable LivingEntity living) {
        if (living instanceof Player player) {
            player.displayClientMessage(
                    Component.translatable("tips.superbwarfare.fh77bw.not_stopped").withStyle(ChatFormatting.RED),
                    true
            );
        }
    }

    @Override
    public void beforeShoot(@Nullable LivingEntity living, @Nullable String weaponName) {
    }

    @Override
    public void vehicleShoot(@Nullable LivingEntity living, @NotNull String weaponName, @Nullable Vec3 targetPos) {
        if ("Main".equals(weaponName) && this.mortarBlockedByMovement()) {
            this.warnNotStopped(living);
            return;
        }
        super.vehicleShoot(living, weaponName, targetPos);
    }

    @Override
    public void vehicleShoot(@Nullable LivingEntity living, @Nullable UUID uuid, @Nullable Vec3 targetPos) {
        String name = living == null ? "Main" : this.getGunName(this.getSeatIndex(living));
        if ("Main".equals(name) && this.mortarBlockedByMovement()) {
            this.warnNotStopped(living);
            return;
        }
        super.vehicleShoot(living, uuid, targetPos);
    }

    @Override
    public boolean canShoot(@Nullable LivingEntity living) {
        if (living != null && living == this.getNthEntity(this.getTurretControllerIndex()) && this.mortarBlockedByMovement()) {
            this.warnNotStopped(living);
            return false;
        }
        return super.canShoot(living);
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
