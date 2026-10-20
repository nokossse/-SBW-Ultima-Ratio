package com.nokosse.ultimaratio.entity.projectile;

import com.atsuishio.superbwarfare.client.animation.entity.BasicProjectileAnimationInstance;
import com.atsuishio.superbwarfare.entity.projectile.Ru3m14MissileEntity;
import com.atsuishio.superbwarfare.resource.model.ProjectileModelReloadListener;
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.animation.BedrockAnimation;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakedBedrockModel;
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HadesMissileEntity extends Ru3m14MissileEntity {
    static final ResourceLocation MODEL_FILE =
            ResourceLocation.fromNamespaceAndPath("ultimaratio", "models/bedrock/projectile/hades_missile.geo.json");
    static final ResourceLocation MODEL_ID =
            ResourceLocation.fromNamespaceAndPath("ultimaratio", "hades_missile");
    static final ResourceLocation MODEL_SHORT =
            ResourceLocation.fromNamespaceAndPath("ultimaratio", "projectile/hades_missile");
    static final ResourceLocation ANIM_FILE =
            ResourceLocation.fromNamespaceAndPath("ultimaratio", "animations/bedrock/projectile/hades_missile.animation.json");

    public HadesMissileEntity(EntityType<? extends HadesMissileEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    /**
     * Ru3m14's client ctor NPE's if this key is missing from the animation map.
     * Keep an empty list in place so the entity can spawn even if geo bake failed.
     */
    public static void ensureClientAssets() {
        var animations = ProjectileModelReloadListener.INSTANCE.getAnimations();
        if (animations.get(ANIM_FILE) == null) {
            animations.put(ANIM_FILE, List.<BedrockAnimation>of());
        }
    }

    @Override
    public BakedModelInstance getModelInstance() {
        BakedBedrockModel baked = firstModel(MODEL_FILE, MODEL_ID, MODEL_SHORT);
        if (baked == null) {
            return super.getModelInstance();
        }
        return baked.createInstance();
    }

    private static BakedBedrockModel firstModel(ResourceLocation... keys) {
        for (ResourceLocation key : keys) {
            BakedBedrockModel baked = ProjectileModelReloadListener.INSTANCE.getModel(key);
            if (baked != null) {
                return baked;
            }
        }
        return null;
    }

    @Override
    public @Nullable BasicProjectileAnimationInstance<?> getAnimationInstance() {
        return null;
    }

    @Override
    public int getHiddenTicks() {
        return -1;
    }

    @Override
    public int getFlareHiddenTicks() {
        return 0;
    }
}
