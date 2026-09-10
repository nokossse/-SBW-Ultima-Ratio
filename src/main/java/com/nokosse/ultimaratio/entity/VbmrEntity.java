package com.nokosse.ultimaratio.entity;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class VbmrEntity extends VehicleEntity {
    /** Raise or lower every seated player. 0 = JSON seat height as-is. */
    public static final float SEAT_Y = -0.5F;

    public VbmrEntity(EntityType<? extends VbmrEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void passengerPos(Entity passenger, Entity.MoveFunction callback, Vec3 vec3, String transform) {
        super.passengerPos(passenger, callback, vec3.add(0.0, SEAT_Y, 0.0), transform);
    }
}
