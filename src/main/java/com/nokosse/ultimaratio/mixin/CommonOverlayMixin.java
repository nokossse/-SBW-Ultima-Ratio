package com.nokosse.ultimaratio.mixin;

import com.atsuishio.superbwarfare.client.overlay.CommonOverlay;
import com.atsuishio.superbwarfare.client.overlay.VehicleHudOverlay;
import com.atsuishio.superbwarfare.client.overlay.VehicleMainWeaponHudOverlay;
import com.nokosse.ultimaratio.entity.Vsr700Entity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While flying a VSR700 only two SBW overlays are drawn: the one that hosts the third-person helicopter HUD and
 * lock frames (VehicleMainWeaponHudOverlay) and the status one (VehicleHudOverlay: energy and health bottom
 * left, weapons bottom right). No crosshair, ammo bar, team or warning overlays.
 */
@Mixin(value = CommonOverlay.class, remap = false)
public abstract class CommonOverlayMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void ultimaratio$onlyHelicopterHud(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
        if (Vsr700Entity.clientPiloted != null
                && !((Object) this instanceof VehicleMainWeaponHudOverlay)
                && !((Object) this instanceof VehicleHudOverlay)) {
            ci.cancel();
        }
    }
}
