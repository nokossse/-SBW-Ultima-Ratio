package com.nokosse.ultimaratio.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.nokosse.ultimaratio.event.VsrRemote;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Unique;

/**
 * Entities are sent to a VSR700 operator by their distance to the drone, not to the operator's body. A drone
 * operator gets them out to the view distance whatever their type's own tracking range (128 blocks for a mob).
 */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class ChunkMapTrackedEntityMixin {
    @Unique
    private ServerPlayer ultimaratio$viewer;

    @Inject(method = "updatePlayer(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"))
    private void ultimaratio$rememberViewer(ServerPlayer player, CallbackInfo ci) {
        this.ultimaratio$viewer = player;
    }

    @Redirect(
            method = "updatePlayer(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkMap$TrackedEntity;getEffectiveRange()I")
    )
    private int ultimaratio$farRange(@Coerce Object tracked) {
        return VsrRemote.flying(ultimaratio$viewer) != null ? Integer.MAX_VALUE : getEffectiveRange();
    }

    @Shadow
    abstract int getEffectiveRange();

    @WrapOperation(
            method = "updatePlayer(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;position()Lnet/minecraft/world/phys/Vec3;")
    )
    private Vec3 ultimaratio$viewPosition(ServerPlayer player, Operation<Vec3> original) {
        Entity view = VsrRemote.viewEntity(player);
        return view == player ? original.call(player) : view.position();
    }
}
