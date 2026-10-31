package com.nokosse.ultimaratio.mixin;

import com.atsuishio.superbwarfare.client.sound.VehicleSoundInstance;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.nokosse.ultimaratio.entity.Vsr700Entity;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The VSR700's engine is full volume within 3 blocks of the listener and 10 times quieter from 8 blocks on. */
@Mixin(value = VehicleSoundInstance.EngineSound.class, remap = false)
public abstract class EngineSoundMixin {
    @Inject(method = "getVolume", at = @At("RETURN"), cancellable = true)
    private void ultimaratio$vsrQuiet(VehicleEntity vehicle, CallbackInfoReturnable<Float> info) {
        if (!(vehicle instanceof Vsr700Entity) || vehicle == Vsr700Entity.clientPiloted) {
            return;
        }
        double distance = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().distanceTo(vehicle.position());
        info.setReturnValue(info.getReturnValue() * (float) Mth.lerp(Mth.clamp((distance - 3.0) / 5.0, 0.0, 1.0), 1.0, 0.1));
    }
}
