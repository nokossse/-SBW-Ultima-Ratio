package com.nokosse.ultimaratio.mixin;

import com.nokosse.ultimaratio.event.VsrRemote;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Entities are sent to a VSR700 operator by their distance to the drone, not to the operator's body. */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class ChunkMapTrackedEntityMixin {
    @Redirect(
            method = "updatePlayer(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;position()Lnet/minecraft/world/phys/Vec3;")
    )
    private Vec3 ultimaratio$viewPosition(ServerPlayer player) {
        return VsrRemote.viewEntity(player).position();
    }
}
