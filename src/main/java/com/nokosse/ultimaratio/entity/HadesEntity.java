package com.nokosse.ultimaratio.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import com.atsuishio.superbwarfare.tools.ParticleTool;

public class HadesEntity extends MambaEntity {
    private static final double SILO_LENGTH = 6.72;

    public HadesEntity(EntityType<? extends HadesEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public void beforeShoot(@Nullable LivingEntity living, @Nullable String weaponName) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 exit = this.getShootPos("Main", 1.0F);
        Vec3 vec = this.getShootVec("Main", 1.0F);
        Vec3 entry = exit.subtract(vec.normalize().scale(SILO_LENGTH));
        ParticleTool.spawnBigCannonMuzzleParticles(vec.scale(-1.0), entry, serverLevel, this);
    }
}
