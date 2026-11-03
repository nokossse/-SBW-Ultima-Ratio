package com.nokosse.ultimaratio.mixin;

import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.nokosse.ultimaratio.event.VsrRemote;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A weapon with AddShooterDeltaMovement (bombs, missiles) inherits the speed of the shooter's root vehicle: for a
 * remote operator that is the drone, not their body standing still (the bomb would drop straight down).
 */
@Mixin(value = GunItem.class, remap = false)
public abstract class GunItemShootMixin {
    @Redirect(
            method = "shootBullet(Lcom/atsuishio/superbwarfare/data/gun/ShootParameters;)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getRootVehicle()Lnet/minecraft/world/entity/Entity;", remap = true)
    )
    private Entity ultimaratio$droneAsVehicle(Entity shooter) {
        return shooter instanceof ServerPlayer player && VsrRemote.flying(player) != null
                ? VsrRemote.flying(player) : shooter.getRootVehicle();
    }
}
