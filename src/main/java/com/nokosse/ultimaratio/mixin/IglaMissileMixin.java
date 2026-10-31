package com.nokosse.ultimaratio.mixin;

import com.atsuishio.superbwarfare.entity.projectile.IglaMissileEntity;
import com.nokosse.ultimaratio.entity.Vsr700Entity;
import com.nokosse.ultimaratio.event.VsrRemote;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A Mistral fired by a VSR700 follows its cursor (where the drone points), like SBW's wire guided 9M120 does for a
 * seated helicopter pilot: that missile reads {@code owner.getVehicle()}, which is empty for a remote operator.
 */
@Mixin(value = IglaMissileEntity.class, remap = false)
public abstract class IglaMissileMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void ultimaratio$followCursor(CallbackInfo info) {
        IglaMissileEntity missile = (IglaMissileEntity) (Object) this;
        if (missile.level().isClientSide || missile.tickCount <= 0 || !(missile.getOwner() instanceof ServerPlayer player)) {
            return;
        }
        Vsr700Entity vsr = VsrRemote.flying(player);
        if (vsr == null || !missile.getTargetUUID().equals("none")) {
            return;
        }
        missile.setDeltaMovement(missile.getDeltaMovement().scale(0.5).add(missile.getLookAngle().scale(1.5)));
        Vec3 aim = vsr.getViewVector(1.0F).scale(1.6);
        Vec3 line = missile.position().subtract(vsr.getShootPosForHud(player, 1.0F)).normalize();
        missile.turn(aim.subtract(line), turnSpeed((missile.tickCount - 1) * 0.4F));
    }

    private static float turnSpeed(float value) {
        return Math.max(0.0F, Math.min(6.0F, value));
    }
}
