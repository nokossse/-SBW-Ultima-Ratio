package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** AS350 Ecureuil: unarmed SBW helicopter, everything is in the vehicle JSON. */
public class As350Entity extends VehicleEntity {
    public As350Entity(EntityType<? extends As350Entity> type, Level level) {
        super(type, level);
    }
}
