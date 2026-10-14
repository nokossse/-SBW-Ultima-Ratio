package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class KeraxEntity extends VehicleEntity {
    public KeraxEntity(EntityType<? extends KeraxEntity> type, Level level) {
        super(type, level);
    }
}
