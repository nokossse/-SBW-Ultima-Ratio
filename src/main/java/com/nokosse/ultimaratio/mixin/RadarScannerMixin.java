package com.nokosse.ultimaratio.mixin;

import com.atsuishio.superbwarfare.tools.RadarScanner;
import com.nokosse.ultimaratio.entity.RadarPicture;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Every SBW radar scan (blocks and vehicles alike) also tells the SAMP/T which enemy missiles it saw. */
@Mixin(value = RadarScanner.class, remap = false)
public abstract class RadarScannerMixin {
    @Inject(method = "scan", at = @At("RETURN"), remap = false)
    private void ultimaratio$shareMissiles(ServerLevel level, RadarScanner.RadarConfig config,
                                           CallbackInfoReturnable<RadarScanner.ScanResult> cir) {
        RadarPicture.report(level, config.getOwner(), cir.getReturnValue().getHostileIds());
    }
}
