package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** EC665 Tigre: SBW helicopter flight model (like the Mi-28), everything is in the vehicle JSON. */
public class TigreEntity extends VehicleEntity {
    public TigreEntity(EntityType<? extends TigreEntity> type, Level level) {
        super(type, level);
    }
}
