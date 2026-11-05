package com.nokosse.ultimaratio.client;

import com.seibel.distanthorizons.api.DhApi;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataCache;
import com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataRepo;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiLevelWrapper;
import com.seibel.distanthorizons.api.interfaces.world.IDhApiWorldProxy;
import com.seibel.distanthorizons.api.objects.DhApiResult;
import com.seibel.distanthorizons.api.objects.data.DhApiTerrainDataPoint;
import com.nokosse.ultimaratio.UltimaRatio;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.BooleanSupplier;

/**
 * Raycast on Distant Horizons' terrain data, beyond the chunks the client holds. Only loaded when DH is installed
 * (LaserTrace), and only called from LaserTrace's worker thread: DH reads its data from disk and waits for it.
 * <p>
 * DH's own {@code terrainRepo.raycast} gives up on the whole ray at the first column it has no data for, which is
 * most rays over a few hundred blocks: this one walks the ray column by column (one block wide) and skips them.
 */
final class DhLaser {
    /** Safety net only: a cold section takes seconds to load from disk, and the ray must outlast that. */
    private static final long TIME_BUDGET_NANOS = 60_000_000_000L;
    private static final long LOG_INTERVAL_NANOS = 2_000_000_000L;
    /** One cache per worker thread (LaserTrace runs one thread per lane): the API gives no word on sharing one. */
    private static final ThreadLocal<IDhApiTerrainDataCache> CACHE = new ThreadLocal<>();
    private static final ThreadLocal<IDhApiLevelWrapper> CACHE_LEVEL = new ThreadLocal<>();
    private static long lastLog;

    /** Where the ray meets DH's terrain: the block, and the exact point on its face. */
    record Hit(BlockPos block, Vec3 location) {
    }

    private DhLaser() {
    }

    @Nullable
    static Hit raycast(Vec3 from, Vec3 dir, int range, BooleanSupplier superseded) {
        try {
            IDhApiWorldProxy world = DhApi.Delayed.worldProxy;
            IDhApiTerrainDataRepo repo = DhApi.Delayed.terrainRepo;
            if (world == null || repo == null || !world.worldLoaded()) {
                log("DH world not loaded");
                return null;
            }
            IDhApiLevelWrapper level = world.getSinglePlayerLevel();
            if (level == null) {
                log("DH has no level for the player");
                return null;
            }
            if (level != CACHE_LEVEL.get()) { // the cache is per dimension
                CACHE.set(repo.createSoftCache());
                CACHE_LEVEL.set(level);
            }
            return march(repo, level, CACHE.get(), from, dir, range, superseded);
        } catch (RuntimeException e) {
            UltimaRatio.LOGGER.warn("Distant Horizons laser raycast failed", e);
            return null;
        }
    }

    /** Column by column along the ray (2D DDA over x/z), first solid DH data point the ray goes through. */
    @Nullable
    private static Hit march(IDhApiTerrainDataRepo repo, IDhApiLevelWrapper level, IDhApiTerrainDataCache cache, Vec3 from,
                             Vec3 dir, int range, BooleanSupplier superseded) {
        long start = System.nanoTime();
        int x = Mth.floor(from.x);
        int z = Mth.floor(from.z);
        int stepX = dir.x > 0 ? 1 : -1;
        int stepZ = dir.z > 0 ? 1 : -1;
        double deltaX = dir.x == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dir.x);
        double deltaZ = dir.z == 0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / dir.z);
        double nextX = dir.x == 0 ? Double.POSITIVE_INFINITY : (dir.x > 0 ? x + 1 - from.x : from.x - x) * deltaX;
        double nextZ = dir.z == 0 ? Double.POSITIVE_INFINITY : (dir.z > 0 ? z + 1 - from.z : from.z - z) * deltaZ;
        int minY = level.getMinHeight();
        int maxY = level.getMaxHeight();
        double tIn = 0;
        int columns = 0;
        int missing = 0;
        String firstMissing = null;
        while (tIn <= range) {
            double tOut = Math.min(Math.min(nextX, nextZ), range);
            double yIn = from.y + dir.y * tIn;
            double yOut = from.y + dir.y * tOut;
            if ((yIn < minY && yOut < minY) || (yIn > maxY && yOut > maxY && dir.y >= 0)) {
                break; // left the world through the bottom, or climbing above it: nothing more to hit
            }
            columns++;
            DhApiResult<DhApiTerrainDataPoint[]> column = repo.getColumnDataAtBlockPos(level, x, z, cache);
            if (!column.success || column.payload == null) {
                missing++;
                if (firstMissing == null) {
                    firstMissing = column.message;
                }
            } else {
                Hit hit = hitInColumn(column.payload, x, z, from, dir, tIn, tOut);
                if (hit != null) {
                    log(String.format("hit %s at %.0f m (%d columns, %d without data, %d ms)", hit.block.toShortString(),
                            hit.location.distanceTo(from), columns, missing, (System.nanoTime() - start) / 1_000_000));
                    return hit;
                }
            }
            if (superseded.getAsBoolean()) {
                return null;
            }
            if (System.nanoTime() - start > TIME_BUDGET_NANOS) {
                log(String.format("gave up after %d columns (%.0f m), DH too slow", columns, tIn));
                return null;
            }
            if (nextX < nextZ) {
                tIn = nextX;
                nextX += deltaX;
                x += stepX;
            } else {
                tIn = nextZ;
                nextZ += deltaZ;
                z += stepZ;
            }
        }
        log(String.format("no hit within %d m (%d columns, %d without data%s)", range, columns, missing,
                firstMissing == null ? "" : ": " + firstMissing));
        return null;
    }

    /** The ray crosses column (x, z) between tIn and tOut: first non-air data point whose height range it passes through. */
    @Nullable
    private static Hit hitInColumn(DhApiTerrainDataPoint[] points, int x, int z, Vec3 from, Vec3 dir, double tIn, double tOut) {
        double best = Double.POSITIVE_INFINITY;
        DhApiTerrainDataPoint bestPoint = null;
        for (DhApiTerrainDataPoint point : points) {
            IDhApiBlockStateWrapper block = point == null ? null : point.blockStateWrapper;
            if (block == null || block.isAir()) {
                continue;
            }
            double lo;
            double hi;
            if (dir.y == 0) {
                if (from.y < point.bottomYBlockPos || from.y > point.topYBlockPos) {
                    continue;
                }
                lo = tIn;
                hi = tOut;
            } else {
                double ta = (point.bottomYBlockPos - from.y) / dir.y;
                double tb = (point.topYBlockPos - from.y) / dir.y;
                lo = Math.max(tIn, Math.min(ta, tb));
                hi = Math.min(tOut, Math.max(ta, tb));
            }
            if (lo <= hi && lo < best) {
                best = lo;
                bestPoint = point;
            }
        }
        if (bestPoint == null) {
            return null;
        }
        Vec3 location = from.add(dir.scale(best));
        int y = Mth.clamp(Mth.floor(location.y), bestPoint.bottomYBlockPos, Math.max(bestPoint.bottomYBlockPos, bestPoint.topYBlockPos - 1));
        return new Hit(new BlockPos(x, y, z), location);
    }

    /** Debug log, throttled: the range trace runs every frame the mouse moves. */
    private static void log(String message) {
        long now = System.nanoTime();
        if (now - lastLog > LOG_INTERVAL_NANOS) {
            lastLog = now;
            UltimaRatio.LOGGER.debug("[UR laser/DH] {}", message);
        }
    }
}
