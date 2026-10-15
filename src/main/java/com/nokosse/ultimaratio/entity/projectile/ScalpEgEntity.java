package com.nokosse.ultimaratio.entity.projectile;

import com.atsuishio.superbwarfare.entity.projectile.Ru3m14MissileEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** SCALP-EG: flies exactly like the 3M14 Kalibr; model/animation are auto-loaded from the entity id. */
public class ScalpEgEntity extends Ru3m14MissileEntity {
    public ScalpEgEntity(EntityType<? extends ScalpEgEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }
}
