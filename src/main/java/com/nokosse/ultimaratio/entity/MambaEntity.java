package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationContext;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance;
import com.atsuishio.superbwarfare.entity.vehicle.base.SpArtilleryEntity;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class MambaEntity extends SpArtilleryEntity {
    public static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(MambaEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> PHASE_TICKS =
            SynchedEntityData.defineId(MambaEntity.class, EntityDataSerializers.INT);

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
    }

    public int getPhaseTicks() {
        return this.entityData.get(PHASE_TICKS);
    }

    public void setPhaseTicks(int ticks) {
        this.entityData.set(PHASE_TICKS, ticks);
    }

    public boolean isReadyToFire() {
        return this.getPhase() == PHASE_READY;
    }

    public boolean blocksDriving() {
        return this.getPhase() != PHASE_IDLE;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PHASE, PHASE_IDLE);
        this.entityData.define(PHASE_TICKS, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Phase", this.getPhase());
        compound.putInt("PhaseTicks", this.getPhaseTicks());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Phase")) {
            this.setPhase(compound.getInt("Phase"));
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
        return this.isReadyToFire() && super.canShoot(living);
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
