package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationContext;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

import java.util.HashMap;

public class SampTNgEntity extends VehicleEntity {
    public static final EntityDataAccessor<Boolean> DEPLOYED =
            SynchedEntityData.defineId(SampTNgEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Boolean> POWERED =
            SynchedEntityData.defineId(SampTNgEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int OPERATOR_SEAT = 1;

    public static final String DEPLOY_ANIM = "deploy_radar";
    public static final String FOLD_ANIM = "fold_radar";
    public static final int DEPLOY_ANIM_TICKS = 60;

    private final MobileRadar mobileRadar = new MobileRadar();
    private boolean clientDeployed;
    private int clientDeployStartTick = -1;
    private boolean clientPowered;
    private int clientPowerStartTick = -1;

    public SampTNgEntity(EntityType<? extends SampTNgEntity> type, Level level) {
        super(type, level);
    }

    public static boolean isSrsLoaded() {
        return MambaRadarEntity.isSrsLoaded();
    }

    public boolean isRadarDeployed() {
        return this.entityData.get(DEPLOYED);
    }

    public void setRadarDeployed(boolean deployed) {
        this.entityData.set(DEPLOYED, deployed);
        if (!deployed) {
            this.setRadarPowered(false);
        }
    }

    public boolean isRadarPowered() {
        return this.entityData.get(POWERED);
    }

    public void setRadarPowered(boolean powered) {
        this.entityData.set(POWERED, powered);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DEPLOYED, false);
        this.entityData.define(POWERED, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("RadarDeployed", this.isRadarDeployed());
        compound.putBoolean("RadarPowered", this.isRadarPowered());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("RadarDeployed")) {
            this.setRadarDeployed(compound.getBoolean("RadarDeployed"));
        }
        if (compound.contains("RadarPowered")) {
            this.setRadarPowered(compound.getBoolean("RadarPowered"));
        }
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (!this.level().isClientSide) {
            this.mobileRadar.tick(this);
            if (!isSrsLoaded()) {
                this.tickFallbackDeploy();
            }
        } else if (isSrsLoaded()) {
            this.tickSrsPowerClient();
        } else {
            this.tickDeployAnimation();
        }
    }

    private void tickFallbackDeploy() {
        boolean occupied = this.hasRadarOperator();
        if (occupied != this.isRadarDeployed()) {
            this.setRadarDeployed(occupied);
        }
    }

    private boolean hasRadarOperator() {
        return this.getNthEntity(OPERATOR_SEAT) != null;
    }

    private void tickSrsPowerClient() {
        boolean powered = this.isRadarPowered();
        if (powered == this.clientPowered) {
            return;
        }
        this.clientPowered = powered;
        this.clientPowerStartTick = powered ? this.tickCount : -1;
    }

    private void tickDeployAnimation() {
        boolean deployed = this.isRadarDeployed();
        if (deployed == this.clientDeployed) {
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
        if (deployed) {
            this.stopClip(context, FOLD_ANIM, 2);
            this.playClip(context, DEPLOY_ANIM, AnimationPlayType.PLAY_ONCE_HOLD, 2);
            this.clientDeployStartTick = this.tickCount;
        } else {
            this.stopClip(context, DEPLOY_ANIM, 2);
            this.playClip(context, FOLD_ANIM, AnimationPlayType.PLAY_ONCE_HOLD, 2);
            this.clientDeployStartTick = -1;
        }
        this.clientDeployed = deployed;
    }

    public boolean isRadarDishSpinning() {
        if (isSrsLoaded()) {
            return this.isRadarDeployed() && this.isRadarPowered();
        }
        return this.isRadarDeployed()
                && this.clientDeployStartTick >= 0
                && this.tickCount - this.clientDeployStartTick >= DEPLOY_ANIM_TICKS;
    }

    public float getRadarDishYawRad(float partialTick) {
        if (!this.isRadarDishSpinning()) {
            return 0.0F;
        }
        int startTick = isSrsLoaded() ? this.clientPowerStartTick : this.clientDeployStartTick;
        float delay = isSrsLoaded() ? 0.0F : DEPLOY_ANIM_TICKS;
        if (startTick < 0) {
            return 0.0F;
        }
        float age = this.tickCount - startTick - delay + partialTick;
        return Math.max(0.0F, age) * ((float) Math.PI / 10.0F);
    }

    private void playClip(
            VehicleAnimationContext<?> context,
            String name,
            AnimationPlayType type,
            int fade
    ) {
        context.playAnimation(this.resolveClip(context, name), type, fade);
    }

    private void stopClip(VehicleAnimationContext<?> context, String name, int fade) {
        context.stopAnimation(this.resolveClip(context, name), fade);
    }

    private String resolveClip(VehicleAnimationContext<?> context, String name) {
        HashMap<String, BedrockAnimation> animations = context.getAnimations();
        if (animations.containsKey(name)) {
            return name;
        }
        if (name.startsWith("animation.")) {
            String stripped = name.substring("animation.".length());
            if (animations.containsKey(stripped)) {
                return stripped;
            }
        } else {
            String prefixed = "animation." + name;
            if (animations.containsKey(prefixed)) {
                return prefixed;
            }
        }
        return name;
    }
}
