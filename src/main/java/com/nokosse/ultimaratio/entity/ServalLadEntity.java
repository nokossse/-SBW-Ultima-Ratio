package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.entity.projectile.SwarmDroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.event.GunEventHandler;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.RangeTool;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.UUID;

/** VBMR-L Serval LAD: with nobody in the gunner seat and auto mode on, the ARX30 shoots hostile drones and phantoms around it. */
public class ServalLadEntity extends ServalEntity {
    public static final EntityDataAccessor<Boolean> AUTO =
            SynchedEntityData.defineId(ServalLadEntity.class, EntityDataSerializers.BOOLEAN);

    /** Entity id of the engaged target, synced so the client turns the turret the same way as the server. */
    public static final EntityDataAccessor<Integer> TARGET =
            SynchedEntityData.defineId(ServalLadEntity.class, EntityDataSerializers.INT);

    private static final String CANNON = "Cannon";
    private static final double AUTO_RANGE = 50.0;
    /** The shot must be this close (degrees) to the target vector before the gun fires. */
    private static final double FIRE_ANGLE = 2.0;

    private UUID owner;

    public ServalLadEntity(EntityType<? extends ServalLadEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(AUTO, false);
        this.entityData.define(TARGET, -1);
    }

    public boolean isAuto() {
        return this.entityData.get(AUTO);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Auto", this.isAuto());
        if (this.owner != null) {
            compound.putUUID("AutoOwner", this.owner);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(AUTO, compound.getBoolean("Auto"));
        if (compound.hasUUID("AutoOwner")) {
            this.owner = compound.getUUID("AutoOwner");
        }
    }

    /** Same as the CIWS: shift + right click toggles the automatic turret (first user becomes the owner). */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isCrouching() && !this.isWreck() && !this.getLocked()
                && !player.getMainHandItem().is(ModTags.Items.TOOLS_CROWBAR)) {
            if (this.owner == null) {
                this.owner = player.getUUID();
            }
            if (!player.getUUID().equals(this.owner)) {
                return InteractionResult.PASS;
            }
            if (!this.level().isClientSide) {
                boolean auto = !this.isAuto();
                this.entityData.set(AUTO, auto);
                player.displayClientMessage(Component.translatable(auto
                        ? "tips.superbwarfare.auto_aimable_entity.active"
                        : "tips.superbwarfare.auto_aimable_entity.inactive"), true);
                this.level().playSound(null, player.blockPosition(), SoundEvents.ARROW_HIT_PLAYER,
                        SoundSource.PLAYERS, 0.5F, 1.0F);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        return super.interact(player, hand);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        this.tickAutoTurret();
    }

    /** Runs on both sides: the server picks and syncs the target, each side then turns the turret towards it. */
    private void tickAutoTurret() {
        boolean server = !this.level().isClientSide;
        if (!this.isAuto() || this.isWreck() || this.getNthEntity(this.getTurretControllerIndex()) != null) {
            if (server && this.entityData.get(TARGET) != -1) {
                this.entityData.set(TARGET, -1);
            }
            return;
        }

        Player ownerPlayer = this.owner == null ? null : this.level().getPlayerByUUID(this.owner);
        Vec3 muzzle = this.getShootPos(CANNON, 1.0F);
        if (server) {
            Entity current = this.level().getEntity(this.entityData.get(TARGET));
            if (current == null || !this.isAutoTarget(current, ownerPlayer, muzzle)) {
                Entity next = this.level()
                        .getEntities(this, this.getBoundingBox().inflate(AUTO_RANGE), e -> this.isAutoTarget(e, ownerPlayer, muzzle))
                        .stream()
                        .min(Comparator.comparingDouble(this::distanceToSqr))
                        .orElse(null);
                this.entityData.set(TARGET, next == null ? -1 : next.getId());
            }
        }

        Entity target = this.level().getEntity(this.entityData.get(TARGET));
        if (target == null) {
            return;
        }
        Vec3 targetPos = target.getBoundingBox().getCenter();
        Vec3 aim = RangeTool.calculateFiringSolution(muzzle, targetPos, target.getDeltaMovement(),
                this.getProjectileVelocity(CANNON), this.getProjectileGravity(CANNON));
        this.turretAutoAimFromVector(aim);
        if (!server) {
            return;
        }

        if (this.getAmmoCount(CANNON) <= 0) {
            this.modifyGunData(CANNON, data -> GunEventHandler.INSTANCE.tryStartReload(this.getAmmoSupplier(), data, true));
            return;
        }
        int shotInterval = (int) Math.ceil(20.0 / (this.vehicleWeaponRpm(CANNON) / 60.0));
        if (this.tickCount % shotInterval == 0
                && VectorTool.calculateAngle(this.getShootVec(CANNON, 1.0F), aim) < FIRE_ANGLE) {
            this.vehicleShoot(ownerPlayer, CANNON, targetPos);
        }
    }

    private boolean isAutoTarget(Entity e, @Nullable Player ownerPlayer, Vec3 muzzle) {
        if (!e.isAlive() || e.distanceToSqr(this) > AUTO_RANGE * AUTO_RANGE) {
            return false;
        }
        boolean hostile;
        if (e instanceof Phantom) {
            hostile = true;
        } else if (e instanceof DroneEntity drone) {
            // an uncontrolled drone that is not flying is not a threat
            hostile = !drone.onGround() && !this.isFriendly(ownerPlayer, drone.getController(), drone);
        } else if (e instanceof SwarmDroneEntity swarm) {
            Entity shooter = swarm.getOwner();
            hostile = shooter == null || !this.isFriendly(ownerPlayer, shooter, swarm);
        } else {
            return false;
        }
        return hostile && this.level().clip(new ClipContext(muzzle, e.getBoundingBox().getCenter(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.BLOCK;
    }

    private boolean isFriendly(@Nullable Player ownerPlayer, @Nullable Entity controller, Entity target) {
        if (ownerPlayer == null) {
            return false;
        }
        return controller == ownerPlayer
                || (controller != null && (SeekTool.IN_SAME_TEAM.test(ownerPlayer, controller) || controller.isAlliedTo(ownerPlayer)))
                || SeekTool.IS_FRIENDLY.test(ownerPlayer, target);
    }
}
