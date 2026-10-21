package com.nokosse.ultimaratio.entity.projectile;

import com.atsuishio.superbwarfare.entity.projectile.MediumRocketEntity;
import com.atsuishio.superbwarfare.resource.model.ProjectileModelReloadListener;
import com.atsuishio.superbwarfare.world.saveddata.ProjectileChunkSavedData;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakedBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class ThundartRocketEntity extends MediumRocketEntity {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath("ultimaratio", "projectile/thundart_rocket");
    public ThundartRocketEntity(EntityType<? extends ThundartRocketEntity> type, Level level) {
        super(type, level);
        this.setType(MediumRocketEntity.Type.HE);
        this.setExplosionDamage(650.0F);
        this.setExplosionRadius(22.0F);
    }

    /** Unguided ballistic round: apogee can be above build height on long shots, so keep the path loaded. */
    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel server) || this.getY() <= server.getMaxBuildHeight()) {
            return;
        }
        Vec3 motion = this.getDeltaMovement();
        for (double k = 0.0; k <= 2.0; k += 1.0) {
            ProjectileChunkSavedData.queueForceLoad(server, new ChunkPos(BlockPos.containing(this.position().add(motion.scale(k)))));
        }
    }

    @Override
    public BakedModelInstance getModelInstance() {
        BakedBedrockModel baked = ProjectileModelReloadListener.INSTANCE.getModel(MODEL);
        if (baked == null) {
            return super.getModelInstance();
        }
        return baked.createInstance();
    }

    @Override
    public int getHiddenTicks() {
        return 0;
    }
}
