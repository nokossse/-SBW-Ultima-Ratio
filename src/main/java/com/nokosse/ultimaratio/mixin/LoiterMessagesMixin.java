package com.nokosse.ultimaratio.mixin;

import com.atsuishio.superbwarfare.network.message.send.LoiterConfigMessage;
import com.atsuishio.superbwarfare.network.message.send.LoiterOverrideMessage;
import com.nokosse.ultimaratio.event.VsrRemote;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** SBW's loiter packets reach the Reaper its operator flies, or the one picked on the tactical map (VsrRemote.loiterVehicleOf). */
@Mixin(value = {LoiterConfigMessage.class, LoiterOverrideMessage.class}, remap = false)
public abstract class LoiterMessagesMixin {
    @Redirect(
            method = "handler",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getVehicle()Lnet/minecraft/world/entity/Entity;", remap = true)
    )
    private Entity ultimaratio$vehicle(ServerPlayer player) {
        return VsrRemote.loiterVehicleOf(player);
    }
}
