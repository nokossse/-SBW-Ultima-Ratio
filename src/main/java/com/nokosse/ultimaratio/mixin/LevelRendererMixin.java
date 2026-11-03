package com.nokosse.ultimaratio.mixin;

import com.nokosse.ultimaratio.entity.Vsr700Entity;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Vanilla centres the rendered chunk area on the player's body: centre it on the drone while flying. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    /** The camera counts as detached while flying, so the local player's body is drawn even in first person. */
    @Redirect(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;isDetached()Z"))
    private boolean ultimaratio$showOwnBody(Camera camera) {
        return Vsr700Entity.clientPiloted != null || camera.isDetached();
    }
}
