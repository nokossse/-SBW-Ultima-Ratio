package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.block.entity.FuMO25BlockEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.tools.RadarScanner;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Radar mobile : memes stats que le GF300 (portee x1.25 du FuMO25, secteur 150, un tour / 5 s), actif quand la place 3
 * est occupee et qu'il reste de l'energie. Operateur = joueur en place 3 ; personne = radar eteint ; consommation x2.
 */
final class MobileRadar {
    private static final int CYCLE_TICKS = 100;
    private static final double RANGE_MULT = 1.25;
    private static final double SWEEP_ANGLE = 150.0;

    private static final int OPERATOR_SEAT = 2;

    private boolean active;

    void tick(VehicleEntity vehicle) {
        if (!(vehicle.level() instanceof ServerLevel level)) return;
        // radar actif seulement si la place 3 (index 2) est occupee par un joueur : c'est lui l'operateur
        if (!(vehicle.getNthEntity(OPERATOR_SEAT) instanceof Player player)) {
            active = false;
            return;
        }

        int cost = FuMO25BlockEntity.DEFAULT_ENERGY_COST * 2;
        active = vehicle.getEnergy() >= (active ? cost : FuMO25BlockEntity.DEFAULT_MIN_ENERGY);
        if (!active) return;

        vehicle.consumeEnergy(cost);
        Vec3 center = vehicle.position().add(0, 3.5, 0);
        double yRot = (vehicle.tickCount % CYCLE_TICKS) * (360.0 / CYCLE_TICKS);
        String sourceId = "vehicle_" + vehicle.getId();

        var vehicles = new RadarScanner.RadarConfig(player, center, 1024 * RANGE_MULT, SWEEP_ANGLE, yRot, 0.0,
                RadarScanner.SearchType.VEHICLES, null, null, true, true, sourceId, true);
        RadarScanner.INSTANCE.sendRadarConfig(vehicles, level);
        RadarScanner.INSTANCE.scan(level, vehicles).sendToClients(player, level, vehicles.getShareWithTeammates());

        var living = new RadarScanner.RadarConfig(player, center, 128 * RANGE_MULT, SWEEP_ANGLE, yRot, 0.0,
                RadarScanner.SearchType.LIVING, null, null, true, true, sourceId, true);
        RadarScanner.INSTANCE.scan(level, living).sendToClients(player, level, living.getShareWithTeammates());
    }
}
