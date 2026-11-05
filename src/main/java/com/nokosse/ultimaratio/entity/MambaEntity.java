package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType;
import com.nokosse.ultimaratio.UltimaRatioConfig;
import com.nokosse.ultimaratio.init.ModGameRules;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationContext;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance;
import com.atsuishio.superbwarfare.entity.vehicle.base.SpArtilleryEntity;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.ChatFormatting;
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
import java.util.stream.Stream;

public class MambaEntity extends SpArtilleryEntity {
    public static final EntityDataAccessor<Integer> PHASE =
            SynchedEntityData.defineId(MambaEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> PHASE_TICKS =
            SynchedEntityData.defineId(MambaEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Boolean> DEPLOYED =
            SynchedEntityData.defineId(MambaEntity.class, EntityDataSerializers.BOOLEAN);

    public static final EntityDataAccessor<Boolean> AUTO =
            SynchedEntityData.defineId(MambaEntity.class, EntityDataSerializers.BOOLEAN);

    /** Aircraft and vehicles are engaged out to this distance (missiles: see MISSILE_RANGE and the radar picture). */
    private static final double AUTO_RANGE = 512.0;
    /** Safety net: an enemy missile this close is engaged even with no radar covering it (radar-spotted ones have no limit). */
    private static final double MISSILE_RANGE = 1000.0;
    private static final int AUTO_COOLDOWN_TICKS = 60;
    private static final int AUTO_RETARGET_TICKS = 200;
    /** The idle search (a wide entity query) runs every other tick. */
    private static final int AUTO_SEARCH_INTERVAL = 2;

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
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PHASE, PHASE_IDLE);
        this.entityData.define(PHASE_TICKS, 0);
        this.entityData.define(DEPLOYED, false);
        this.entityData.define(AUTO, false);
    }

    public boolean isAuto() {
        return this.entityData.get(AUTO);
    }

    /** Launchers that can fire on their own (the MAMBA); the HADES is a surface-to-surface missile and cannot. */
    protected boolean hasAutoMode() {
        return true;
    }

    /** Length of a launch tube: the back blast comes out of its rear end. */
    protected double siloLength() {
        return 4.94375;
    }

    /**
     * Like the CIWS: shift + right click from outside toggles ACTIVE/INACTIVE. The first user becomes the owner; the
     * owner and their team may switch it.
     */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.hasAutoMode() && player.isCrouching() && !this.isWreck() && !this.getLocked()
                && !player.getMainHandItem().is(ModTags.Items.TOOLS_CROWBAR)) {
            if (this.owner == null) {
                this.owner = player.getUUID();
            }
            if (!OwnerTeam.canControl(this.level(), this.owner, player)) {
                if (!this.level().isClientSide) {
                    player.displayClientMessage(Component.translatable("tips.ultimaratio.auto.other_team")
                            .withStyle(ChatFormatting.RED), true);
                }
                return InteractionResult.sidedSuccess(this.level().isClientSide);
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
        if (!this.hasAutoMode() || !this.isAuto() || !this.isDeployed() || this.isWreck()
                || this.tickCount % AUTO_SEARCH_INTERVAL != 0
                || this.getNthEntity(this.getTurretControllerIndex()) != null
                || this.tickCount - this.lastAutoShotTick < AUTO_COOLDOWN_TICKS
                || this.getAmmoCount("Main") <= 0) {
            return;
        }
        Player ownerPlayer = this.owner == null ? null : this.level().getPlayerByUUID(this.owner);
        double minY = this.getY() + UltimaRatioConfig.SAMPT_MIN_TARGET_HEIGHT.get();
        // enemy missiles: the whole team's radar picture, anywhere on the map, not just what is near the launcher
        Stream<Entity> radarMissiles = ownerPlayer == null || !(this.level() instanceof ServerLevel server)
                ? Stream.empty() : RadarPicture.missilesFor(server, ownerPlayer).stream();
        Entity target = Stream.concat(this.level()
                        .getEntities(this, this.getBoundingBox().inflate(MISSILE_RANGE),
                                e -> this.isAutoTarget(e, ownerPlayer, minY)
                                        && (!(e instanceof MissileProjectile) || e.distanceToSqr(this) <= MISSILE_RANGE * MISSILE_RANGE))
                        .stream(), radarMissiles.filter(e -> this.isAutoTarget(e, ownerPlayer, minY)))
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
        // a missile is a threat at any height and range (it dives under minY on the way in)
        if (!e.isAlive() || !(e instanceof MissileProjectile)
                && (e.getY() < minY || e.distanceToSqr(this) > AUTO_RANGE * AUTO_RANGE)) {
            return false;
        }
        // do not stack missiles on a target already engaged
        if (e.getUUID().equals(this.lastAutoTarget) && this.tickCount - this.lastAutoShotTick < AUTO_RETARGET_TICKS) {
            return false;
        }
        if (e instanceof Enemy) {
            return true;
        }
        boolean allies = this.level().getGameRules().getBoolean(ModGameRules.SAMPT_TARGET_ALLIES);
        // SBW missiles only (no bullets, no swarm drones, never our own ASTER-30), not fired by an ally (unless the game rule says so)
        if (e instanceof MissileProjectile m) {
            if (m instanceof SwarmDroneEntity || m instanceof Aster30MissileEntity) {
                return false;
            }
            // no shooter (e.g. /summon) counts as hostile
            Entity shooter = m.getOwner();
            return shooter == null || (shooter != ownerPlayer && (allies || ownerPlayer == null || !shooter.isAlliedTo(ownerPlayer)));
        }
        // enemy vehicle: crewed and not friendly to the owner (unless the game rule says so)
        return e instanceof VehicleEntity v && v != this && !v.getPassengers().isEmpty()
                && ownerPlayer != null && (allies || !SeekTool.IS_FRIENDLY.test(ownerPlayer, v))
                && v.getPassengers().stream().noneMatch(p -> p == ownerPlayer);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Phase", this.getPhase());
        compound.putInt("PhaseTicks", this.getPhaseTicks());
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
        Vec3 entry = exit.subtract(vec.normalize().scale(this.siloLength()));
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
