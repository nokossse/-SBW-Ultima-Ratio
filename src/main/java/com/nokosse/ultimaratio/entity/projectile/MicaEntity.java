package com.nokosse.ultimaratio.entity.projectile;

import com.atsuishio.superbwarfare.entity.projectile.Ru9m100MissileEntity;
import com.atsuishio.superbwarfare.resource.model.ProjectileModelReloadListener;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakedBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** MICA: reuses SBW's missile guidance (Ru9m100), only the model differs. */
public class MicaEntity extends Ru9m100MissileEntity {
    private static final ResourceLocation MODEL = new ResourceLocation("ultimaratio", "projectile/mica");

    public MicaEntity(EntityType<? extends MicaEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public BakedModelInstance getModelInstance() {
        BakedBedrockModel baked = ProjectileModelReloadListener.INSTANCE.getModel(MODEL);
        return baked == null ? super.getModelInstance() : baked.createInstance();
    }

    @Override
    public int getHiddenTicks() {
        return 0;
    }
}
