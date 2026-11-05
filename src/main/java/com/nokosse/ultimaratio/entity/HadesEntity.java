package com.nokosse.ultimaratio.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * HADES: a surface-to-surface missile launcher on the MAMBA's deployable chassis. Same stabilisation and deployment
 * cycle, longer tubes, and no automatic mode: the gunner lays every shot.
 */
public class HadesEntity extends MambaEntity {
    public HadesEntity(EntityType<? extends HadesEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected boolean hasAutoMode() {
        return false;
    }

    @Override
    protected double siloLength() {
        return 6.72;
    }
}
