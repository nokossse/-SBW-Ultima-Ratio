package com.nokosse.ultimaratio.init;

import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.entity.CaesarEntity;
import com.nokosse.ultimaratio.entity.MambaEntity;
import com.nokosse.ultimaratio.entity.MambaRadarEntity;
import com.nokosse.ultimaratio.entity.LeclercXlrEntity;
import com.nokosse.ultimaratio.entity.VbmrMepacEntity;
import com.nokosse.ultimaratio.entity.VbmrEntity;
import com.nokosse.ultimaratio.entity.projectile.Aster30MissileEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTRY =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, UltimaRatio.MODID);

    public static final RegistryObject<EntityType<CaesarEntity>> CAESAR = register(
            "caesar",
            EntityType.Builder.<CaesarEntity>of(CaesarEntity::new, MobCategory.MISC)
                    .setTrackingRange(512)
                    .setUpdateInterval(1)
                    .fireImmune()
                    .sized(4.5F, 3.4F)
    );

    public static final RegistryObject<EntityType<MambaEntity>> MAMBA = register(
            "mamba",
            EntityType.Builder.<MambaEntity>of(MambaEntity::new, MobCategory.MISC)
                    .setTrackingRange(512)
                    .setUpdateInterval(1)
                    .fireImmune()
                    .sized(4.5F, 3.4F)
    );

    public static final RegistryObject<EntityType<MambaRadarEntity>> MAMBA_RADAR = register(
            "mamba_radar",
            EntityType.Builder.<MambaRadarEntity>of(MambaRadarEntity::new, MobCategory.MISC)
                    .setTrackingRange(512)
                    .setUpdateInterval(1)
                    .fireImmune()
                    .sized(4.5F, 3.4F)
    );

    public static final RegistryObject<EntityType<VbmrEntity>> VBMR = register(
            "vbmr",
            EntityType.Builder.<VbmrEntity>of(VbmrEntity::new, MobCategory.MISC)
                    .setTrackingRange(512)
                    .setUpdateInterval(1)
                    .fireImmune()
                    .sized(4.0F, 3.2F)
    );

    public static final RegistryObject<EntityType<VbmrMepacEntity>> VBMR_MEPAC = register(
            "vbmr_mepac",
            EntityType.Builder.<VbmrMepacEntity>of(VbmrMepacEntity::new, MobCategory.MISC)
                    .setTrackingRange(512)
                    .setUpdateInterval(1)
                    .fireImmune()
                    .sized(4.0F, 3.2F)
    );

    public static final RegistryObject<EntityType<LeclercXlrEntity>> LECLERC_XLR = register(
            "leclerc_xlr",
            EntityType.Builder.<LeclercXlrEntity>of(LeclercXlrEntity::new, MobCategory.MISC)
                    .setTrackingRange(512)
                    .setUpdateInterval(1)
                    .fireImmune()
                    .sized(5.2F, 3.5F)
    );

    public static final RegistryObject<EntityType<Aster30MissileEntity>> ASTER_30_MISSILE = register(
            "aster_30_missile",
            EntityType.Builder.<Aster30MissileEntity>of(Aster30MissileEntity::new, MobCategory.MISC)
                    .setTrackingRange(512)
                    .setUpdateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .fireImmune()
                    .sized(0.5F, 0.5F)
    );

    private static <T extends Entity> RegistryObject<EntityType<T>> register(
            String name,
            EntityType.Builder<T> builder
    ) {
        return REGISTRY.register(name, () -> builder.build(name));
    }
}
