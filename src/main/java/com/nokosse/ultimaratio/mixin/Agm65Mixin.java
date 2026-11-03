package com.nokosse.ultimaratio.mixin;

import com.atsuishio.superbwarfare.entity.projectile.Agm65Entity;
import com.nokosse.ultimaratio.entity.Mq9ReaperEntity;
import com.nokosse.ultimaratio.event.VsrRemote;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The MQ-9's Hellfire has no lock (mq9_reaper.json): once its motor lights it rides the operator's camera, zoomed or
 * not (ReaperAimMessage), like the VSR700's Mistral rides the drone's nose (IglaMissileMixin).
 */
@Mixin(value = Agm65Entity.class, remap = false)
public abstract class Agm65Mixin {
    /** SBW's own: the motor lights at tick 8. */
    private static final int MOTOR_TICK = 8;
    /** How far ahead of the missile, along the camera's line, it steers: smaller hugs the line tighter. */
    private static final double LEAD = 40.0;

    @Inject(method = {"tick", "m_8119_"}, at = @At("TAIL"))
    private void ultimaratio$rideCamera(CallbackInfo info) {
        Agm65Entity missile = (Agm65Entity) (Object) this;
        if (missile.level().isClientSide || missile.tickCount <= MOTOR_TICK || !(missile.getOwner() instanceof ServerPlayer player)
                || !(VsrRemote.flying(player) instanceof Mq9ReaperEntity reaper) || !missile.getTargetUUID().equals("none")) {
            return;
        }
        Vec3[] aim = reaper.aim();
        if (aim == null) {
            return;
        }
        // Beam riding: a point on the camera's line a little past the missile, so it closes on the line and flies along it
        Vec3 from = aim[0];
        Vec3 dir = aim[1];
        Vec3 ahead = from.add(dir.scale(missile.position().distanceTo(from) + LEAD));
        missile.turn(ahead.subtract(missile.position()), Math.min(6.0F, (missile.tickCount - MOTOR_TICK) * 0.4F));
        missile.setLostTargetTick(0); // SBW blows up a missile with no target after 100 ticks
    }
}
