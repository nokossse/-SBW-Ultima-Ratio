package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity;
import com.atsuishio.superbwarfare.client.animation.AnimationPlayType;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationContext;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModTags;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.HashMap;
import java.util.UUID;

/**
 * A radar truck of the SAMP/T battery (ARABEL, NG): no weapons; shift + right click deploys the radar (it then scans
 * through {@link MobileRadar}) or folds it. It needs energy to deploy and folds by itself once the energy runs out.
 */
public abstract class RadarVehicleEntity extends VehicleEntity {
    public static final EntityDataAccessor<Boolean> DEPLOYED =
            SynchedEntityData.defineId(RadarVehicleEntity.class, EntityDataSerializers.BOOLEAN);

    private final MobileRadar mobileRadar;
    private final String deployAnim;
    private final String foldAnim;
    /** Length of the deploy clip: the dish only starts turning once it is up. */
    private final int deployAnimTicks;
    /** Whoever switched the radar on (saved): the scan goes to them when nobody sits in the truck. */
    private UUID radarActivator;
    private boolean clientDeployed;
    private int clientDeployStartTick = -1;

    protected RadarVehicleEntity(EntityType<? extends RadarVehicleEntity> type, Level level, ModConfigSpec.DoubleValue range,
                                 String deployAnim, String foldAnim, int deployAnimTicks) {
        super(type, level);
        this.mobileRadar = new MobileRadar(range);
        this.deployAnim = deployAnim;
        this.foldAnim = foldAnim;
        this.deployAnimTicks = deployAnimTicks;
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
        if (!player.isCrouching() || this.isWreck() || this.getLocked()
                || player.getMainHandItem().is(ModTags.Items.TOOLS_CROWBAR)) {
            return super.interact(player, hand);
        }
        if (!this.level().isClientSide) {
            boolean deploy = !this.isRadarDeployed();
            if (deploy && this.getEnergy() < FuMO25BlockEntity.DEFAULT_MIN_ENERGY) {
                player.displayClientMessage(Component.translatable("tips.ultimaratio.radar.no_energy")
                        .withStyle(ChatFormatting.RED), true);
                return InteractionResult.CONSUME;
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

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DEPLOYED, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("RadarDeployed", this.isRadarDeployed());
        if (this.radarActivator != null) {
            compound.putUUID("RadarActivator", this.radarActivator);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("RadarDeployed")) {
            this.setRadarDeployed(compound.getBoolean("RadarDeployed"));
        }
        this.radarActivator = compound.hasUUID("RadarActivator") ? compound.getUUID("RadarActivator") : null;
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
        VehicleAnimationContext<?> context = anim == null ? null : anim.getContext();
        if (context == null) {
            return;
        }
        if (deployed) {
            context.stopAnimation(this.resolveClip(context, this.foldAnim), 2);
            context.playAnimation(this.resolveClip(context, this.deployAnim), AnimationPlayType.PLAY_ONCE_HOLD, 2);
            this.clientDeployStartTick = this.tickCount;
        } else {
            context.stopAnimation(this.resolveClip(context, this.deployAnim), 2);
            context.playAnimation(this.resolveClip(context, this.foldAnim), AnimationPlayType.PLAY_ONCE_HOLD, 2);
            this.clientDeployStartTick = -1;
        }
        this.clientDeployed = deployed;
    }

    /** Client: the radar is up (its deploy clip has played) and its antenna turns. */
    public boolean isRadarDishSpinning() {
        return this.isRadarDeployed()
                && this.clientDeployStartTick >= 0
                && this.tickCount - this.clientDeployStartTick >= this.deployAnimTicks;
    }

    /** Client: antenna yaw, one turn every 20 ticks from the moment it is up. */
    public float getRadarDishYawRad(float partialTick) {
        if (!this.isRadarDishSpinning()) {
            return 0.0F;
        }
        float age = this.tickCount - this.clientDeployStartTick - this.deployAnimTicks + partialTick;
        return Math.max(0.0F, age) * ((float) Math.PI / 10.0F);
    }

    /** Exported clips are named with or without the "animation." prefix depending on the model. */
    private String resolveClip(VehicleAnimationContext<?> context, String name) {
        HashMap<String, BedrockAnimation> animations = context.getAnimations();
        if (animations.containsKey(name)) {
            return name;
        }
        String other = name.startsWith("animation.") ? name.substring("animation.".length()) : "animation." + name;
        return animations.containsKey(other) ? other : name;
    }
}
