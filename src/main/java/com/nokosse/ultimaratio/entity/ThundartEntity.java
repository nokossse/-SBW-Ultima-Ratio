package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationContext;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.entity.vehicle.base.SpArtilleryEntity;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.UUID;

/**
 * M142 HIMARS-style launcher. Laying comes from SBW's own artillery-indicator
 * solution ({@code setTarget}/{@code resetTarget}); rate of fire is the weapon's RPM.
 * Rockets are unguided: a precise ballistic arc, no terminal guidance.
 */
public class ThundartEntity extends SpArtilleryEntity {
    private static final double MOVING_SQR = 0.007;

    private static final String SHOOT_ANIM = "animation.thundart.shoot";
    private static final String SILO_ANIM_PREFIX = "animation.thundart.shoot.silo";

    private boolean wasMoving;
    private int clientAmmo = -1;

    public ThundartEntity(EntityType<? extends ThundartEntity> type, Level level) {
        super(type, level);
    }

    private boolean isMoving() {
        return this.getDeltaMovement().horizontalDistanceSqr() > MOVING_SQR;
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (this.level().isClientSide) {
            this.tickSiloReload();
            return;
        }
        if (this.isWreck()) {
            return;
        }
        this.tickStow();
    }

    /** Driving: pod travels stowed over the cab. Stopping: crew re-lays on the current fire mission. */
    private void tickStow() {
        Entity gunner = this.getNthEntity(this.getTurretControllerIndex());
        boolean moving = this.isMoving();
        if (moving && gunner == null) {
            this.setLockTurret(false);
            this.setShootVec(Vec3.directionFromRotation(0.0F, this.getYRot()).toVector3f());
        } else if (!moving && this.wasMoving && this.hasMission()) {
            this.resetTarget("Main");
        }
        this.wasMoving = moving;
    }

    private boolean hasMission() {
        var pos = this.getTargetPos();
        return pos.getX() != 0 || pos.getZ() != 0;
    }

    /** SBW's 4-arg overload skips the muzzle blast that the 3-arg ones add, so add it here. */
    @Override
    public void vehicleShoot(
            @Nullable LivingEntity living,
            @NotNull String weaponName,
            @Nullable UUID uuid,
            @Nullable Vec3 targetPos
    ) {
        this.beforeShoot(living, weaponName);
        super.vehicleShoot(living, weaponName, uuid, targetPos);
    }

    // --- client visuals: pod recoil + per-tube lid, reset on reload ---

    /** BoundUpWithAmmoAmount sends fire index {@code ammo - 1}; index 0 = tube 1. */
    public void playClientShot(int fireIndex) {
        VehicleAnimationContext<?> context = this.animContext();
        if (context == null) {
            return;
        }
        this.playClip(context, SHOOT_ANIM, AnimationPlayType.PLAY_ONCE_STOP);
        int silo = fireIndex + 1;
        if (silo >= 1 && silo <= 8) {
            this.playClip(context, SILO_ANIM_PREFIX + silo, AnimationPlayType.PLAY_ONCE_HOLD);
        }
    }

    private void tickSiloReload() {
        GunData gun = this.getGunData("Main");
        int ammo = gun == null ? 0 : gun.ammo.get();
        if (this.clientAmmo >= 0 && ammo > this.clientAmmo) {
            VehicleAnimationContext<?> context = this.animContext();
            if (context != null) {
                for (int i = 1; i <= 8; i++) {
                    context.stopAnimation(this.resolveClip(context, SILO_ANIM_PREFIX + i), 2);
                }
            }
        }
        this.clientAmmo = ammo;
    }

    private @Nullable VehicleAnimationContext<?> animContext() {
        VehicleAnimationInstance<?> anim = this.getAnim();
        return anim == null ? null : anim.getContext();
    }

    private void playClip(VehicleAnimationContext<?> context, String name, AnimationPlayType type) {
        context.playAnimation(this.resolveClip(context, name), type, 0);
    }

    private String resolveClip(VehicleAnimationContext<?> context, String name) {
        HashMap<String, BedrockAnimation> animations = context.getAnimations();
        if (animations.containsKey(name)) {
            return name;
        }
        String stripped = name.substring("animation.".length());
        return animations.containsKey(stripped) ? stripped : name;
    }
}
