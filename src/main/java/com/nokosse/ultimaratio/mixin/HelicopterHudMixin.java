package com.nokosse.ultimaratio.mixin;

import com.atsuishio.superbwarfare.client.overlay.weapon.HelicopterHud;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.nokosse.ultimaratio.entity.Vsr700Entity;
import net.minecraft.client.CameraType;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.objectweb.asm.Opcodes;

/** The VSR700 operator always gets SBW's third-person helicopter HUD, even in first person. */
@Mixin(value = HelicopterHud.class, remap = false)
public abstract class HelicopterHudMixin {
    @Redirect(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getCameraType()Lnet/minecraft/client/CameraType;", remap = true)
    )
    private CameraType ultimaratio$thirdPersonHud(Options options) {
        return Vsr700Entity.clientPiloted != null ? CameraType.THIRD_PERSON_BACK : options.getCameraType();
    }

    /** Zooming must not switch to the cockpit-style HUD either. */
    @Redirect(
            method = "render",
            at = @At(value = "FIELD", target = "Lcom/atsuishio/superbwarfare/event/ClientEventHandler;zoomVehicle:Z", opcode = Opcodes.GETSTATIC)
    )
    private boolean ultimaratio$noZoomHud() {
        return Vsr700Entity.clientPiloted == null && ClientEventHandler.zoomVehicle;
    }
}
