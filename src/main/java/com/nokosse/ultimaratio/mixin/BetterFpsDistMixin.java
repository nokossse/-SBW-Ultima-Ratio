package com.nokosse.ultimaratio.mixin;

import com.nokosse.ultimaratio.entity.Vsr700Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** betterfpsdist hides Embeddium sections far from the local player's body: measure from the drone while flying. */
@Pseudo
@Mixin(targets = "com.betterfpsdist.event.ClientEventHandler", remap = false)
public abstract class BetterFpsDistMixin {
    private static final String TARGET = "adjustedDistance(IIIDDD)D";

    @ModifyVariable(method = TARGET, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static double ultimaratio$droneX(double playerX) {
        return Vsr700Entity.clientPiloted != null ? Vsr700Entity.clientPiloted.getX() : playerX;
    }

    @ModifyVariable(method = TARGET, at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private static double ultimaratio$droneY(double playerY) {
        return Vsr700Entity.clientPiloted != null ? Vsr700Entity.clientPiloted.getY() : playerY;
    }

    @ModifyVariable(method = TARGET, at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private static double ultimaratio$droneZ(double playerZ) {
        return Vsr700Entity.clientPiloted != null ? Vsr700Entity.clientPiloted.getZ() : playerZ;
    }
}
