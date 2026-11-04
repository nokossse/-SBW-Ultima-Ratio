package com.nokosse.ultimaratio.mixin;

import com.nokosse.ultimaratio.event.VsrRemote;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.EntityAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Chunk loading, chunk sending and spawning follow a player's "view entity" instead of the player:
 * the VSR700 they fly, or themselves (see VsrRemote.viewEntity).
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
    /** 1.21: the chunk view (ChunkTrackingView) is centred on chunkPosition() in updateChunkTracking. */
    @WrapOperation(
            method = "updateChunkTracking(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;chunkPosition()Lnet/minecraft/world/level/ChunkPos;")
    )
    private ChunkPos ultimaratio$viewChunk(ServerPlayer player, Operation<ChunkPos> original) {
        Entity view = VsrRemote.viewEntity(player);
        return view == player ? original.call(player) : view.chunkPosition();
    }

    @WrapOperation(
            method = {
                    "move(Lnet/minecraft/server/level/ServerPlayer;)V",
                    "updatePlayerStatus(Lnet/minecraft/server/level/ServerPlayer;Z)V",
                    "updatePlayerPos(Lnet/minecraft/server/level/ServerPlayer;)V"
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/SectionPos;of(Lnet/minecraft/world/level/entity/EntityAccess;)Lnet/minecraft/core/SectionPos;")
    )
    private SectionPos ultimaratio$viewSection(EntityAccess entity, Operation<SectionPos> original) {
        return original.call(entity instanceof ServerPlayer player && VsrRemote.viewEntity(player) != player ? VsrRemote.viewEntity(player) : entity);
    }

    @Redirect(
            method = "euclideanDistanceSquared(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/entity/Entity;)D",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getX()D")
    )
    private static double ultimaratio$viewX(Entity entity) {
        return entity instanceof ServerPlayer player ? VsrRemote.viewEntity(player).getX() : entity.getX();
    }

    @Redirect(
            method = "euclideanDistanceSquared(Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/entity/Entity;)D",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getZ()D")
    )
    private static double ultimaratio$viewZ(Entity entity) {
        return entity instanceof ServerPlayer player ? VsrRemote.viewEntity(player).getZ() : entity.getZ();
    }
}
