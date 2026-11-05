package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.entity.projectile.MissileProjectile;
import com.atsuishio.superbwarfare.tools.SeekTool;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Enemy missiles any radar reported lately (RadarScannerMixin feeds it from SBW's own scan, so FuMO-25, GF300, Mamba
 * radar... all count), with the radar owner so a SAMP/T only reads the picture of its own side. Server thread only.
 */
public final class RadarPicture {
    /** A radar turn lasts 100 ticks in this addon: a track must outlive it or missiles blink between sweeps. */
    private static final int TTL_TICKS = 140;

    private record Track(UUID radarOwner, long seen) {}

    private static final Map<Integer, Track> TRACKS = new HashMap<>();

    private RadarPicture() {}

    public static void report(ServerLevel level, Player radarOwner, List<Integer> hostileIds) {
        // purged here too: with no SAMP/T on auto nobody reads the picture, and old tracks would pile up all session
        forgetOld(level);
        for (int id : hostileIds) {
            if (level.getEntity(id) instanceof MissileProjectile) {
                TRACKS.put(id, new Track(radarOwner.getUUID(), level.getGameTime()));
            }
        }
    }

    private static void forgetOld(ServerLevel level) {
        // a negative age is a track left by the previous world of this game session: forget it as well
        TRACKS.values().removeIf(t -> level.getGameTime() - t.seen() > TTL_TICKS || level.getGameTime() < t.seen());
    }

    /** Missiles detected by a radar friendly to {@code side}, wherever they are on the map. */
    public static List<Entity> missilesFor(ServerLevel level, Player side) {
        forgetOld(level);
        return TRACKS.entrySet().stream()
                .filter(e -> {
                    Player radar = level.getPlayerByUUID(e.getValue().radarOwner());
                    return radar != null && (radar == side || SeekTool.IS_FRIENDLY.test(side, radar));
                })
                .map(e -> level.getEntity(e.getKey()))
                .filter(m -> m instanceof MissileProjectile && m.isAlive())
                .toList();
    }
}
