package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationContext;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.SpArtilleryEntity;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class CaesarEntity extends SpArtilleryEntity {
    public static final EntityDataAccessor<Boolean> STABILIZED =
            SynchedEntityData.defineId(CaesarEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> STABILIZING =
            SynchedEntityData.defineId(CaesarEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> PACKING =
            SynchedEntityData.defineId(CaesarEntity.class, EntityDataSerializers.BOOLEAN);

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
    private UUID lastShooterUuid;

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

    public boolean blocksDriving() {
        return this.isStabilized() || this.isStabilizing() || this.isPacking();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STABILIZED, false);
        this.entityData.define(STABILIZING, false);
        this.entityData.define(PACKING, false);
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

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isSelfFire(source)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    /** Ignore our own shells and blast, including crowbar fire from outside. */
    private boolean isSelfFire(DamageSource source) {
        if (this.isOwnShooter(source.getEntity())) {
            return true;
        }
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile projectile) {
            return this.isOwnShooter(projectile.getOwner());
        }
        return this.isOwnShooter(direct);
    }

    private boolean isOwnShooter(Entity entity) {
        if (entity == null) {
            return false;
        }
        if (entity == this) {
            return true;
        }
        if (this.lastShooterUuid != null && this.lastShooterUuid.equals(entity.getUUID())) {
            return true;
        }
        return entity.getVehicle() == this || this.hasPassenger(entity) || this.hasIndirectPassenger(entity);
    }

    private void markSelfShot(LivingEntity shooter) {
        if (shooter != null) {
            this.lastShooterUuid = shooter.getUUID();
        }
    }

    @Override
    public boolean canShoot(LivingEntity shooter) {
        return this.readyToFire(shooter) && super.canShoot(shooter);
    }

    @Override
    public void vehicleShoot(LivingEntity shooter, String weaponName, Vec3 targetPos) {
        if (!this.readyToFire(shooter)) {
            return;
        }
        this.markSelfShot(shooter);
        super.vehicleShoot(shooter, weaponName, targetPos);
    }

    @Override
    public void vehicleShoot(LivingEntity shooter, UUID shooterUuid, Vec3 targetPos) {
        if (!this.readyToFire(shooter)) {
            return;
        }
        this.markSelfShot(shooter);
        super.vehicleShoot(shooter, shooterUuid, targetPos);
    }

    @Override
    public InteractionResult onCrowbarInteract(ItemStack stack, Player player, InteractionHand hand) {
        if (!this.readyToFire(player)) {
            return InteractionResult.SUCCESS;
        }
        this.markSelfShot(player);
        return super.onCrowbarInteract(stack, player, hand);
    }

    @Override
    public void vehicleShoot(LivingEntity shooter, String weaponName, UUID shooterUuid, Vec3 targetPos) {
        if (!this.readyToFire(shooter)) {
            return;
        }
        this.markSelfShot(shooter);
        super.vehicleShoot(shooter, weaponName, shooterUuid, targetPos);
    }

    private boolean readyToFire(LivingEntity shooter) {
        if (VectorTool.calculateAngle(this.getUpVec(1.0F), new Vec3(0.0, 1.0, 0.0)) > 1.0) {
            this.warn(shooter, "tips.ultimaratio.caesar.body_tilted");
            return false;
        }
        if (this.getDeltaMovement().lengthSqr() > 0.001 || this.isPacking()) {
            this.warn(shooter, this.isPacking()
                    ? "tips.ultimaratio.caesar.packing"
                    : "tips.superbwarfare.fh77bw.not_stopped");
            return false;
        }
        if (!this.isStabilized()) {
            this.warn(shooter, this.isStabilizing()
                    ? "tips.ultimaratio.caesar.stabilizing"
                    : "tips.ultimaratio.caesar.not_stabilized");
            return false;
        }
        return true;
    }

    private void warn(LivingEntity shooter, String key) {
        if (shooter instanceof Player player) {
            player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
        }
    }
}
