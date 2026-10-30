package com.nokosse.ultimaratio.mixin;

import com.atsuishio.superbwarfare.network.message.send.EditMessage;
import com.atsuishio.superbwarfare.network.message.send.MouseMoveMessage;
import com.atsuishio.superbwarfare.network.message.send.ReloadMessage;
import com.atsuishio.superbwarfare.network.message.send.SwitchVehicleWeaponMessage;
import com.atsuishio.superbwarfare.network.message.send.VehicleFireMessage;
import com.atsuishio.superbwarfare.network.message.send.VehicleMovementMessage;
import com.atsuishio.superbwarfare.network.message.send.ZoomMessage;
import com.nokosse.ultimaratio.event.VsrRemote;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** SBW's pilot packets (keys, mouse, fire, weapon switch, reload, zoom) reach the VSR700 its remote operator flies. */
@Mixin(value = {
        VehicleMovementMessage.class, MouseMoveMessage.class, VehicleFireMessage.class,
        SwitchVehicleWeaponMessage.class, ReloadMessage.class, ZoomMessage.class, EditMessage.class
}, remap = false)
public abstract class SbwPilotMessagesMixin {
    @Redirect(
            method = "handler",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getVehicle()Lnet/minecraft/world/entity/Entity;", remap = true)
    )
    private Entity ultimaratio$vehicle(ServerPlayer player) {
        return VsrRemote.vehicleOf(player);
    }
}
