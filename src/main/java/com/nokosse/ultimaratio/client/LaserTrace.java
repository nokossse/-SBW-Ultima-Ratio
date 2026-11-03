package com.nokosse.ultimaratio.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Where the VSR700/Reaper laser lands: a block of the chunks the client holds, else, with Distant Horizons installed,
 * a block of its LOD terrain up to LOD_RANGE (client side only: what the operator sees, nothing the server uses).
 * seen() reads the distance DH drew at the centre of the screen (instant); trace() walks DH's terrain data (seconds
 * for a section never read), kept for when DH's picture cannot be read.
 */
public final class LaserTrace {
    /** Covers the Hellfire's 12 km guided range with margin; DH answers nothing beyond its own LOD distance anyway. */
    private static final int LOD_RANGE = 16000;
    private static final boolean DH = ModList.get().isLoaded("distanthorizons");
    /** One thread per lane: the lock must not queue behind a slow readout trace. */
    private static final ExecutorService[] WORKERS = {worker("range"), worker("lock")};

    private static ExecutorService worker(String name) {
        return Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "UltimaRatio laser " + name);
            thread.setDaemon(true);
            return thread;
        });
    }

    /** The live range readout, and the trace that picks the block to lock: each lane drops its own older traces. */
    public static final int RANGE = 0;
    public static final int LOCK = 1;
    private static final AtomicIntegerArray EPOCH = new AtomicIntegerArray(2);

    /** location: where the ray meets the block; lod: estimated on DH's simplified terrain. */
    public record Hit(Vec3 location, BlockPos block, boolean lod) {
    }

    private LaserTrace() {
    }

    /** The lane's running trace is not wanted any more (the answer would be thrown away): it stops walking. */
    public static void cancel(int lane) {
        EPOCH.incrementAndGet(lane);
    }

    /** Sentinel of seen(): DH's picture cannot be read this frame, trace() is the way. */
    public static final Hit UNKNOWN = new Hit(Vec3.ZERO, BlockPos.ZERO, true);

    /**
     * What the centre of the screen shows this frame, at once: a loaded block, else the LOD terrain DH drew there
     * (read from its depth texture). Null: sky; UNKNOWN: no DH picture to read. Render thread only, and look must be
     * the camera's own (the centre pixel is read).
     */
    @Nullable
    public static Hit seen(Level level, Vec3 from, Vec3 look, Entity except) {
        Vec3 dir = look.normalize();
        Hit loaded = loaded(level, from, dir, except);
        if (loaded != null || !DH) {
            return loaded;
        }
        double distance = DhDepth.centreDistance();
        if (Double.isNaN(distance)) {
            return UNKNOWN;
        }
        if (distance == DhDepth.SKY || distance > LOD_RANGE) {
            return null;
        }
        Vec3 point = from.add(dir.scale(distance));
        // The point is on the drawn surface: the block is the one just behind it
        return new Hit(point, BlockPos.containing(point.add(dir.scale(0.1))), true);
    }

    /**
     * Loaded blocks answer at once; past them the LOD answers a few frames later, off the render thread. Completes
     * with null when nothing is hit, never exceptionally.
     */
    public static CompletableFuture<Hit> trace(Level level, Vec3 from, Vec3 look, Entity except, int lane) {
        Vec3 dir = look.normalize();
        Hit loaded = loaded(level, from, dir, except);
        if (loaded != null || !DH) {
            return CompletableFuture.completedFuture(loaded);
        }
        int epoch = EPOCH.incrementAndGet(lane);
        return CompletableFuture.supplyAsync(() -> {
            DhLaser.Hit lod = DhLaser.raycast(from, dir, LOD_RANGE, () -> EPOCH.get(lane) != epoch);
            return lod == null ? null : new Hit(lod.location(), lod.block(), true);
        }, WORKERS[lane]).exceptionally(e -> null); // e.g. a DH version without this API: the laser just stays within loaded chunks
    }

    /** The chunks the client holds, up to its render distance. */
    @Nullable
    private static Hit loaded(Level level, Vec3 from, Vec3 dir, Entity except) {
        double reach = Math.max(512.0, Minecraft.getInstance().options.getEffectiveRenderDistance() * 16.0);
        BlockHitResult hit = level.clip(new ClipContext(from, from.add(dir.scale(reach)), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, except));
        return hit.getType() == HitResult.Type.MISS ? null : new Hit(hit.getLocation(), hit.getBlockPos(), false);
    }
}
