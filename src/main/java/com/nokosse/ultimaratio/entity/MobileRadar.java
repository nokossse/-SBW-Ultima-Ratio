package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.tools.RadarScanner;
import com.nokosse.ultimaratio.init.ModGameRules;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import java.util.Comparator;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Radar mobile : secteur 150, un tour / 5 s, portee fixee par vehicule. Actif tant que le vehicule le deploie et qu'il reste
 * de l'energie ; consommation x2.
 */
final class MobileRadar {
    private static final int CYCLE_TICKS = 100;
    private static final double SWEEP_ANGLE = 150.0;

    private static final int OPERATOR_SEAT = 1;
    private static final TicketType<ChunkPos> RADAR_TICKET =
            TicketType.create("ultimaratio_radar", Comparator.comparingLong(ChunkPos::toLong), 60);

    private final net.neoforged.neoforge.common.ModConfigSpec.DoubleValue range;
    private boolean active;

    MobileRadar(net.neoforged.neoforge.common.ModConfigSpec.DoubleValue range) {
        this.range = range;
    }

    boolean isActive() {
        return active;
    }

    /** Switch-driven radar: scans only while {@code deployed}; {@link #isActive()} turns false once energy runs out. */
    void tick(VehicleEntity vehicle, boolean deployed, Player fallback) {
        if (!(vehicle.level() instanceof ServerLevel level)) return;
        if (!deployed || vehicle.isWreck()) {
            active = false;
            return;
        }
        if (!drain(vehicle)) return;
        if (vehicle.tickCount % 5 == 0 && level.getGameRules().getBoolean(ModGameRules.RADAR_CHUNK_LOADING)) {
            ChunkPos here = vehicle.chunkPosition();
            level.getChunkSource().addRegionTicket(RADAR_TICKET, here, 3, here);
        }
        // recipient of the scan: operator seat, then driver, then whoever switched the radar on
        Player player = vehicle.getNthEntity(OPERATOR_SEAT) instanceof Player p ? p
                : vehicle.getNthEntity(0) instanceof Player d ? d : fallback;
        if (player != null) scan(vehicle, level, player);
    }

    void tick(VehicleEntity vehicle) {
        if (!(vehicle.level() instanceof ServerLevel level)) return;
        if (vehicle.isWreck()) {
            active = false;
            return;
        }
        // radar actif seulement si la place 3 (index 2) est occupee par un joueur : c'est lui l'operateur
        if (!(vehicle.getNthEntity(OPERATOR_SEAT) instanceof Player player)) {
            active = false;
            return;
        }
        if (!drain(vehicle)) return;
        scan(vehicle, level, player);
    }

    private boolean drain(VehicleEntity vehicle) {
        int cost = FuMO25BlockEntity.DEFAULT_ENERGY_COST * 2;
        active = vehicle.getEnergy() >= (active ? cost : FuMO25BlockEntity.DEFAULT_MIN_ENERGY);
        if (active) vehicle.consumeEnergy(cost);
        return active;
    }

    private void scan(VehicleEntity vehicle, ServerLevel level, Player player) {
        double range = this.range.get();
        Vec3 center = vehicle.position().add(0, 3.5, 0);
        double yRot = (vehicle.tickCount % CYCLE_TICKS) * (360.0 / CYCLE_TICKS);
        String sourceId = "vehicle_" + vehicle.getId();

        var vehicles = new RadarScanner.RadarConfig(player, center, range, SWEEP_ANGLE, yRot, 0.0,
                RadarScanner.SearchType.VEHICLES, null, null, true, true, sourceId, true);
        RadarScanner.INSTANCE.sendRadarConfig(vehicles, level);
        RadarScanner.INSTANCE.scan(level, vehicles).sendToClients(player, level, vehicles.getShareWithTeammates());

        var living = new RadarScanner.RadarConfig(player, center, Math.min(128.0, range), SWEEP_ANGLE, yRot, 0.0,
                RadarScanner.SearchType.LIVING, null, null, true, true, sourceId, true);
        RadarScanner.INSTANCE.scan(level, living).sendToClients(player, level, living.getShareWithTeammates());
    }
}
