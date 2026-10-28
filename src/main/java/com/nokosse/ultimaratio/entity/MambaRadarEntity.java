package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationContext;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity;
import com.atsuishio.superbwarfare.init.ModTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;


import java.util.HashMap;
import java.util.UUID;

public class MambaRadarEntity extends VehicleEntity {
    public static final EntityDataAccessor<Boolean> DEPLOYED =
            SynchedEntityData.defineId(MambaRadarEntity.class, EntityDataSerializers.BOOLEAN);

    public static final String DEPLOY_ANIM = "animation.samp-t.deploy";
    public static final String FOLD_ANIM = "fold";
    public static final int DEPLOY_ANIM_TICKS = 160;

    private final MobileRadar mobileRadar = new MobileRadar(500.0);
    private UUID radarActivator;
    private boolean clientDeployed;
    private int clientDeployStartTick = -1;

    public MambaRadarEntity(EntityType<? extends MambaRadarEntity> type, Level level) {
        super(type, level);
    }

    public boolean isRadarDeployed() {
        return this.entityData.get(DEPLOYED);
    }

    public void setRadarDeployed(boolean deployed) {
        this.entityData.set(DEPLOYED, deployed);
    }

    /** Shift + right click toggles the radar: active = deployed, inactive = folded. Needs energy to deploy. */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (player.isCrouching() && !this.isWreck() && !this.getLocked()
                && !player.getMainHandItem().is(ModTags.Items.TOOLS_CROWBAR)) {
            if (!this.level().isClientSide) {
                boolean deploy = !this.isRadarDeployed();
                if (deploy && this.getEnergy() < FuMO25BlockEntity.DEFAULT_MIN_ENERGY) {
                    player.displayClientMessage(Component.translatable("tips.superbwarfare.auto_aimable_entity.inactive"), true);
                    return InteractionResult.sidedSuccess(false);
                }
                this.setRadarDeployed(deploy);
                this.radarActivator = deploy ? player.getUUID() : null;
                player.displayClientMessage(Component.translatable(deploy
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DEPLOYED, false);
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
            Player activator = this.radarActivator == null ? null : this.level().getPlayerByUUID(this.radarActivator);
            this.mobileRadar.tick(this, this.isRadarDeployed(), activator);
            if (this.isRadarDeployed() && !this.mobileRadar.isActive()) {
                this.setRadarDeployed(false);
            }
        } else {
            this.tickDeployAnimation();
        }
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
        return this.isRadarDeployed()
                && this.clientDeployStartTick >= 0
                && this.tickCount - this.clientDeployStartTick >= DEPLOY_ANIM_TICKS;
    }

    public float getRadarDishYawRad(float partialTick) {
        if (!this.isRadarDishSpinning()) {
            return 0.0F;
        }
        int startTick = this.clientDeployStartTick;
        float delay = DEPLOY_ANIM_TICKS;
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
