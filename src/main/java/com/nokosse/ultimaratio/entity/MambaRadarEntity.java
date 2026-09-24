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
import net.minecraftforge.fml.ModList;

import java.util.HashMap;

public class MambaRadarEntity extends VehicleEntity {
    public static final EntityDataAccessor<Boolean> DEPLOYED =
            SynchedEntityData.defineId(MambaRadarEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int OPERATOR_SEAT_A = 2;
    public static final int OPERATOR_SEAT_B = 3;

    /** Kept for SRS: play this clip, do not retarget it. */
    public static final String DEPLOY_ANIM = "animation.samp-t.deploy";
    public static final String FOLD_ANIM = "fold";

    private boolean clientDeployed;

    public MambaRadarEntity(EntityType<? extends MambaRadarEntity> type, Level level) {
        super(type, level);
    }

    public static boolean isSrsLoaded() {
        ModList mods = ModList.get();
        return mods.isLoaded("srs")
                || mods.isLoaded("superb_radar_system")
                || mods.isLoaded("sbwsrs");
    }

    public boolean isRadarDeployed() {
        return this.entityData.get(DEPLOYED);
    }

    public void setRadarDeployed(boolean deployed) {
        this.entityData.set(DEPLOYED, deployed);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DEPLOYED, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("RadarDeployed", this.isRadarDeployed());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("RadarDeployed")) {
            this.setRadarDeployed(compound.getBoolean("RadarDeployed"));
        }
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (!this.level().isClientSide) {
            this.tickFallbackDeploy();
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
        return this.getNthEntity(OPERATOR_SEAT_A) != null
                || this.getNthEntity(OPERATOR_SEAT_B) != null;
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
        } else {
            this.stopClip(context, DEPLOY_ANIM, 2);
            this.playClip(context, FOLD_ANIM, AnimationPlayType.PLAY_ONCE_HOLD, 2);
        }
        this.clientDeployed = deployed;
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
