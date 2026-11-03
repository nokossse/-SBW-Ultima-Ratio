package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType;
import com.nokosse.ultimaratio.UltimaRatioConfig;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationContext;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance;
import com.atsuishio.superbwarfare.entity.vehicle.base.SpArtilleryEntity;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import com.atsuishio.superbwarfare.entity.projectile.MissileProjectile;
import com.atsuishio.superbwarfare.entity.projectile.SwarmDroneEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.nokosse.ultimaratio.entity.projectile.Aster30MissileEntity;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.SeekTool;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.UUID;

public class MambaEntity extends SpArtilleryEntity {
    public static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(MambaEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> PHASE_TICKS =
            SynchedEntityData.defineId(MambaEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> DEPLOYED =
            SynchedEntityData.defineId(MambaEntity.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Boolean> AUTO =
            SynchedEntityData.defineId(MambaEntity.class, EntityDataSerializers.BOOLEAN);

    /** Auto-fire only on targets at least this many blocks above the launcher's Y. */
    private static final double AUTO_RANGE = 512.0;
    private static final int AUTO_COOLDOWN_TICKS = 60;
    private static final int AUTO_RETARGET_TICKS = 200;

    private UUID owner;
    private UUID lastAutoTarget;
    private int lastAutoShotTick = -AUTO_RETARGET_TICKS;

    public static final int PHASE_IDLE = 0;
    public static final int PHASE_STAB = 1;
    public static final int PHASE_DEPLOY = 2;
    public static final int PHASE_READY = 3;
    public static final int PHASE_PACK_DEPLOY = 4;
    public static final int PHASE_PACK_STAB = 5;

    private static final String STAB_ANIM = "stabilization";
    private static final String STAB_PACK_ANIM = "stabilization_pack";
    private static final String LAUNCHER_ANIM = "launcher_deploy";
    private static final String LAUNCHER_PACK_ANIM = "launcher_pack";

    private static final int STAB_DURATION_TICKS = 200;
    private static final int DEPLOY_DURATION_TICKS = 247;
    private static final double SILO_LENGTH = 4.94375;

    private int clientPhase = PHASE_IDLE;

    public MambaEntity(EntityType<? extends MambaEntity> type, Level level) {
        super(type, level);
    }

    public int getPhase() {
        return this.entityData.get(PHASE);
    }

    public void setPhase(int phase) {
        this.entityData.set(PHASE, phase);
        this.entityData.set(DEPLOYED, phase == PHASE_READY);
    }

    public int getPhaseTicks() {
        return this.entityData.get(PHASE_TICKS);
    }

    public void setPhaseTicks(int ticks) {
        this.entityData.set(PHASE_TICKS, ticks);
    }

    public boolean isReadyToFire() {
        return this.isDeployed();
    }

    /** Fully deployed battery: cannot drive, can fire. Same as PHASE_READY. */
    public boolean isDeployed() {
        return this.entityData.get(DEPLOYED);
    }

    /** Packed for travel: can drive, cannot fire. Same as PHASE_IDLE. */
    public boolean isUndeployed() {
        return this.getPhase() == PHASE_IDLE;
    }

    public boolean blocksDriving() {
        return !this.isUndeployed();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PHASE, PHASE_IDLE);
        builder.define(PHASE_TICKS, 0);
        builder.define(DEPLOYED, false);
        builder.define(AUTO, false);
    }

    public boolean isAuto() {
        return this.entityData.get(AUTO);
    }

    /** Same as the CIWS: shift + right click from outside toggles ACTIVE/INACTIVE (first user becomes the owner). */
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

    private void tickAutoFire() {
        if (!this.isAuto() || !this.isDeployed() || this.isWreck()
                || this.getNthEntity(this.getTurretControllerIndex()) != null
                || this.tickCount - this.lastAutoShotTick < AUTO_COOLDOWN_TICKS
                || this.getAmmoCount("Main") <= 0) {
            return;
        }
        Player ownerPlayer = this.owner == null ? null : this.level().getPlayerByUUID(this.owner);
        double minY = this.getY() + UltimaRatioConfig.SAMPT_MIN_TARGET_HEIGHT.get();
        Entity target = this.level()
                .getEntities(this, this.getBoundingBox().inflate(AUTO_RANGE), e -> this.isAutoTarget(e, ownerPlayer, minY))
                .stream()
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);
        if (target == null) {
            return;
        }
        this.lastAutoTarget = target.getUUID();
        this.lastAutoShotTick = this.tickCount;
        this.vehicleShoot(ownerPlayer, "Main", target.getUUID(), target.getBoundingBox().getCenter());
    }

    private boolean isAutoTarget(Entity e, @Nullable Player ownerPlayer, double minY) {
        if (!e.isAlive() || e.getY() < minY || e.distanceToSqr(this) > AUTO_RANGE * AUTO_RANGE) {
            return false;
        }
        // do not stack missiles on a target already engaged
        if (e.getUUID().equals(this.lastAutoTarget) && this.tickCount - this.lastAutoShotTick < AUTO_RETARGET_TICKS) {
            return false;
        }
        if (e instanceof Enemy) {
            return true;
        }
        // SBW missiles only (no bullets, no swarm drones, never our own ASTER-30), not fired by an ally
        if (e instanceof MissileProjectile m) {
            if (m instanceof SwarmDroneEntity || m instanceof Aster30MissileEntity) {
                return false;
            }
            // no shooter (e.g. /summon) counts as hostile
            Entity shooter = m.getOwner();
            return shooter == null || (shooter != ownerPlayer && (ownerPlayer == null || !shooter.isAlliedTo(ownerPlayer)));
        }
        // enemy vehicle: crewed and not friendly to the owner
        return e instanceof VehicleEntity v && v != this && !v.getPassengers().isEmpty()
                && ownerPlayer != null && !SeekTool.IS_FRIENDLY.test(ownerPlayer, v)
                && v.getPassengers().stream().noneMatch(p -> p == ownerPlayer);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Phase", this.getPhase());
        compound.putInt("PhaseTicks", this.getPhaseTicks());
        compound.putBoolean("Deployed", this.isDeployed());
        compound.putBoolean("Undeployed", this.isUndeployed());
        compound.putBoolean("Auto", this.isAuto());
        if (this.owner != null) {
            compound.putUUID("AutoOwner", this.owner);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Phase")) {
            this.setPhase(compound.getInt("Phase"));
        }
        this.entityData.set(AUTO, compound.getBoolean("Auto"));
        if (compound.hasUUID("AutoOwner")) {
            this.owner = compound.getUUID("AutoOwner");
        }
        if (compound.contains("PhaseTicks")) {
            this.setPhaseTicks(compound.getInt("PhaseTicks"));
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
        this.tickDeploy();
        if (!this.level().isClientSide) {
            this.tickAutoFire();
        }
        if (this.level().isClientSide) {
            this.tickDeployAnimation();
        }
    }

    private void tickDeploy() {
        if (this.level().isClientSide) {
            return;
        }

        Entity gunner = this.getNthEntity(this.getTurretControllerIndex());
        int phase = this.getPhase();
        this.setLockTurret(true);

        if (phase == PHASE_IDLE) {
            if (gunner != null && this.isImmobile()) {
                this.setPhase(PHASE_STAB);
                this.setPhaseTicks(STAB_DURATION_TICKS);
            }
            return;
        }

        int ticks = this.getPhaseTicks() - 1;
        this.setPhaseTicks(ticks);

        if (phase == PHASE_STAB) {
            if (ticks <= 0) {
                this.setPhase(PHASE_DEPLOY);
                this.setPhaseTicks(DEPLOY_DURATION_TICKS);
            }
            return;
        }

        if (phase == PHASE_DEPLOY) {
            if (ticks <= 0) {
                this.setPhase(PHASE_READY);
                this.setPhaseTicks(0);
            }
            return;
        }

        if (phase == PHASE_READY) {
            return;
        }

        if (phase == PHASE_PACK_DEPLOY) {
            if (ticks <= 0) {
                this.setPhase(PHASE_PACK_STAB);
                this.setPhaseTicks(STAB_DURATION_TICKS);
            }
            return;
        }

        if (phase == PHASE_PACK_STAB && ticks <= 0) {
            this.finishPackUp();
        }
    }

    private boolean isImmobile() {
        return this.getDeltaMovement().horizontalDistanceSqr() < 0.0001
                && !this.forwardInputDown()
                && !this.backInputDown();
    }

    private void requestPackUp() {
        if (this.level().isClientSide) {
            return;
        }
        int phase = this.getPhase();
        if (phase == PHASE_IDLE || phase == PHASE_PACK_DEPLOY || phase == PHASE_PACK_STAB) {
            return;
        }
        this.setLockTurret(true);
        if (phase == PHASE_STAB) {
            this.setPhase(PHASE_PACK_STAB);
            this.setPhaseTicks(STAB_DURATION_TICKS);
            return;
        }
        this.setPhase(PHASE_PACK_DEPLOY);
        this.setPhaseTicks(DEPLOY_DURATION_TICKS);
    }

    private void finishPackUp() {
        this.setPhase(PHASE_IDLE);
        this.setPhaseTicks(0);
        this.setTurretXRot(0.0F);
        this.setTurretYRot(0.0F);
        this.setLockTurret(true);
    }

    private void tickDeployAnimation() {
        int phase = this.getPhase();
        if (phase == this.clientPhase) {
            return;
        }
        int previous = this.clientPhase;
        this.clientPhase = phase;

        VehicleAnimationInstance<?> anim = this.getAnim();
        if (anim == null) {
            return;
        }
        VehicleAnimationContext<?> context = anim.getContext();
        if (context == null) {
            return;
        }

        if (phase == PHASE_STAB) {
            context.stopAnimation(STAB_PACK_ANIM, 0);
            context.playAnimation(STAB_ANIM, AnimationPlayType.PLAY_ONCE_HOLD, 5);
        } else if (phase == PHASE_DEPLOY) {
            context.playAnimation(LAUNCHER_ANIM, AnimationPlayType.PLAY_ONCE_HOLD, 2);
        } else if (phase == PHASE_READY) {
            if (previous != PHASE_DEPLOY) {
                context.playAnimation(STAB_ANIM, AnimationPlayType.PLAY_ONCE_HOLD, 0);
                context.playAnimation(LAUNCHER_ANIM, AnimationPlayType.PLAY_ONCE_HOLD, 0);
            }
        } else if (phase == PHASE_PACK_DEPLOY) {
            context.stopAnimation(LAUNCHER_ANIM, 0);
            context.playAnimation(LAUNCHER_PACK_ANIM, AnimationPlayType.PLAY_ONCE_HOLD, 2);
        } else if (phase == PHASE_PACK_STAB) {
            context.stopAnimation(LAUNCHER_ANIM, 2);
            context.stopAnimation(LAUNCHER_PACK_ANIM, 2);
            context.stopAnimation(STAB_ANIM, 0);
            context.playAnimation(STAB_PACK_ANIM, AnimationPlayType.PLAY_ONCE_HOLD, 2);
        } else if (phase == PHASE_IDLE) {
            context.stopAnimation(STAB_ANIM, 8);
            context.stopAnimation(STAB_PACK_ANIM, 8);
            context.stopAnimation(LAUNCHER_ANIM, 8);
            context.stopAnimation(LAUNCHER_PACK_ANIM, 8);
        }
    }

    @Override
    public void beforeShoot(@Nullable LivingEntity living, @Nullable String weaponName) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 exit = this.getShootPos("Main", 1.0F);
        Vec3 vec = this.getShootVec("Main", 1.0F);
        Vec3 entry = exit.subtract(vec.normalize().scale(SILO_LENGTH));
        ParticleTool.spawnBigCannonMuzzleParticles(vec.scale(-1.0), entry, serverLevel, this);
    }

    @Override
    public boolean canShoot(@Nullable LivingEntity living) {
        return this.isDeployed() && super.canShoot(living);
    }

    @Override
    public boolean canBind() {
        return false;
    }

    @Override
    public float getTurretTurnXSpeed() {
        return 0.0F;
    }

    @Override
    public float getTurretTurnYSpeed() {
        return 0.0F;
    }
}
