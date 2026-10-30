package com.nokosse.ultimaratio.mixin;

import com.nokosse.ultimaratio.event.VsrRemote;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
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
    @Redirect(
            method = {"move(Lnet/minecraft/server/level/ServerPlayer;)V", "updatePlayerStatus(Lnet/minecraft/server/level/ServerPlayer;Z)V"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getBlockX()I")
    )
    private int ultimaratio$viewBlockX(ServerPlayer player) {
        return VsrRemote.viewEntity(player).getBlockX();
    }

    @Redirect(
            method = {"move(Lnet/minecraft/server/level/ServerPlayer;)V", "updatePlayerStatus(Lnet/minecraft/server/level/ServerPlayer;Z)V"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getBlockZ()I")
    )
    private int ultimaratio$viewBlockZ(ServerPlayer player) {
        return VsrRemote.viewEntity(player).getBlockZ();
    }

    @Redirect(
            method = {
                    "move(Lnet/minecraft/server/level/ServerPlayer;)V",
                    "updatePlayerStatus(Lnet/minecraft/server/level/ServerPlayer;Z)V",
                    "updatePlayerPos(Lnet/minecraft/server/level/ServerPlayer;)Lnet/minecraft/core/SectionPos;"
            },
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/SectionPos;of(Lnet/minecraft/world/level/entity/EntityAccess;)Lnet/minecraft/core/SectionPos;")
    )
    private SectionPos ultimaratio$viewSection(EntityAccess entity) {
        return SectionPos.of(entity instanceof ServerPlayer player ? VsrRemote.viewEntity(player) : entity);
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
