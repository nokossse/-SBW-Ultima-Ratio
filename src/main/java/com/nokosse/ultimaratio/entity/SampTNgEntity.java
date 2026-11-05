package com.nokosse.ultimaratio.entity;

import com.nokosse.ultimaratio.UltimaRatioConfig;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** SAMP/T NG: the Ground Fire 300 radar of the new battery on a Renault Kerax, range set by the samptNgRadarRange config. */
public class SampTNgEntity extends RadarVehicleEntity {
    public SampTNgEntity(EntityType<? extends SampTNgEntity> type, Level level) {
        super(type, level, UltimaRatioConfig.SAMPT_NG_RADAR_RANGE, "deploy_radar", "fold_radar", 60);
    }
}
