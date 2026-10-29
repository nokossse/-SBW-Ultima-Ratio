package com.nokosse.ultimaratio.mixin;

import com.atsuishio.superbwarfare.client.screens.TacticalMapScreen;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TacticalMapScreen.class)
public class TacticalMapScreenMixin {
    @Inject(method = "drawFilledSector", at = @At("HEAD"), cancellable = true, remap = false)
    private void ultimaratio$drawCone(GuiGraphics g, float cx, float cy, float radius, float startDeg, float sweepDeg,
                                      CallbackInfo ci) {
        RadarCone.draw(g, cx, cy, radius, startDeg, sweepDeg);
        ci.cancel();
    }
}
