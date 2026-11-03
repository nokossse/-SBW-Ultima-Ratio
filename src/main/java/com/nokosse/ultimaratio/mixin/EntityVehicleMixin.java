package com.nokosse.ultimaratio.mixin;

import com.nokosse.ultimaratio.entity.Vsr700Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Client: while flying a VSR700 the local player reports the drone as their vehicle, so SBW's pilot
 * camera, HUD and controls run as for a seated pilot. Being a "passenger" of a vehicle that does not
 * list them also stops the client from ticking the body: it stays still and sends no movement.
 * An operator who rides something (SBW vehicle, boat, minecart) keeps their real vehicle for what the
 * level does to a passenger (ClientLevelMixin), so they are carried along normally.
 */
@Mixin(Entity.class)
public abstract class EntityVehicleMixin {
    /** A drone operator draws every entity out to the render distance, not just the 64 blocks per size unit vanilla allows. */
    @Inject(method = "shouldRenderAtSqrDistance", at = @At("HEAD"), cancellable = true)
    private void ultimaratio$farRender(double distanceSqr, CallbackInfoReturnable<Boolean> cir) {
        if (Vsr700Entity.clientPiloted != null) {
            double range = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0;
            cir.setReturnValue(distanceSqr < range * range);
        }
    }

    @Inject(method = "getVehicle", at = @At("HEAD"), cancellable = true)
    private void ultimaratio$remoteVehicle(CallbackInfoReturnable<Entity> cir) {
        Vsr700Entity vsr = Vsr700Entity.clientPiloted;
        if (vsr != null && !Vsr700Entity.clientRenderingPilotBody && Vsr700Entity.clientTickingPassenger == 0
                && (Object) this == Vsr700Entity.clientPilot) {
            cir.setReturnValue(vsr);
        }
    }
}
