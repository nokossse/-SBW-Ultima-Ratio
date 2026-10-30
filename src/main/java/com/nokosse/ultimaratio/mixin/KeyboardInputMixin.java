package com.nokosse.ultimaratio.mixin;

import com.nokosse.ultimaratio.entity.Vsr700Entity;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The keys fly the VSR700: a boat or horse the operator rides must not read them as steering. */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends Input {
    @Inject(method = "tick", at = @At("RETURN"))
    private void ultimaratio$noSteering(boolean sneaking, float sneakSpeed, CallbackInfo ci) {
        if (Vsr700Entity.clientPiloted != null) {
            this.up = false;
            this.down = false;
            this.left = false;
            this.right = false;
            this.jumping = false;
            this.shiftKeyDown = false;
            this.forwardImpulse = 0;
            this.leftImpulse = 0;
        }
    }
}
