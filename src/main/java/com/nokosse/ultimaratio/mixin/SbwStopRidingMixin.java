package com.nokosse.ultimaratio.mixin;

import com.atsuishio.superbwarfare.network.message.send.PlayerStopRidingMessage;
import com.nokosse.ultimaratio.event.VsrRemote;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** SBW's dismount key also releases the VSR700 controls: it must not throw the operator out of the vehicle they ride. */
@Mixin(value = PlayerStopRidingMessage.class, remap = false)
public abstract class SbwStopRidingMixin {
    @Inject(method = "handler", at = @At("HEAD"), cancellable = true)
    private void ultimaratio$keepRiding(IPayloadContext context, CallbackInfo ci) {
        ServerPlayer player = context.player() instanceof ServerPlayer p ? p : null;
        if (player != null && VsrRemote.flying(player) != null) {
            ci.cancel();
        }
    }
}
