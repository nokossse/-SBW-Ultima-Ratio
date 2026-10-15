package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationContext;
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** VBMR-L Serval: rear stabilizer stairs lower when stopped and fold up when it moves off. */
public class ServalEntity extends VbmrEntity {
    private static final String STAIRS_DOWN = "animation.stairs_down";
    private static final String STAIRS_UP = "animation.stairs_up";
    private static final double MOVING_SPEED = 0.03;
    private static final int STOP_DELAY_TICKS = 20;

    private int stoppedTicks;
    /** null until the first client tick so a freshly loaded vehicle snaps to its pose. */
    private Boolean stairsDown;

    public ServalEntity(EntityType<? extends ServalEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        if (this.level().isClientSide) {
            this.tickStairs();
        }
    }

    private void tickStairs() {
        boolean moving = this.getDeltaMovement().horizontalDistance() > MOVING_SPEED;
        this.stoppedTicks = moving ? 0 : this.stoppedTicks + 1;
        boolean wantDown = this.stoppedTicks >= STOP_DELAY_TICKS;
        if (this.stairsDown != null && this.stairsDown == wantDown) {
            return;
        }

        VehicleAnimationInstance<?> anim = this.getAnim();
        VehicleAnimationContext<?> context = anim == null ? null : anim.getContext();
        if (context == null) {
            return;
        }

        int blend = this.stairsDown == null ? 0 : 2;
        if (wantDown) {
            context.stopAnimation(STAIRS_UP, 0);
            context.playAnimation(STAIRS_DOWN, AnimationPlayType.PLAY_ONCE_HOLD, blend);
        } else if (this.stairsDown != null) {
            context.stopAnimation(STAIRS_DOWN, 0);
            context.playAnimation(STAIRS_UP, AnimationPlayType.PLAY_ONCE_HOLD, blend);
        }
        this.stairsDown = wantDown;
    }
}
