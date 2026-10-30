package com.nokosse.ultimaratio.mixin;

import com.nokosse.ultimaratio.entity.Vsr700Entity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ticking a passenger checks its vehicle: it must be the real one, or the client would dismount the operator. */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "tickPassenger", at = @At("HEAD"))
    private void ultimaratio$enterPassengerTick(Entity vehicle, Entity passenger, CallbackInfo ci) {
        Vsr700Entity.clientTickingPassenger++;
    }

    @Inject(method = "tickPassenger", at = @At("RETURN"))
    private void ultimaratio$leavePassengerTick(Entity vehicle, Entity passenger, CallbackInfo ci) {
        Vsr700Entity.clientTickingPassenger--;
    }
}
