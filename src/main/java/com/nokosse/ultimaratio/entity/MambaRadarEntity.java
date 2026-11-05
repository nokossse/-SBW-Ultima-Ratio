package com.nokosse.ultimaratio.entity;

import com.nokosse.ultimaratio.UltimaRatioConfig;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** SAMP/T ARABEL: the battery's acquisition radar on a Renault Kerax, range set by the arabelRadarRange config. */
public class MambaRadarEntity extends RadarVehicleEntity {
    public MambaRadarEntity(EntityType<? extends MambaRadarEntity> type, Level level) {
        super(type, level, UltimaRatioConfig.ARABEL_RADAR_RANGE, "animation.samp-t.deploy", "fold", 160);
    }
}
