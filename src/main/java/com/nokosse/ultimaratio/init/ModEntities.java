package com.nokosse.ultimaratio.init;

import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.entity.CaesarEntity;
import com.nokosse.ultimaratio.entity.HadesEntity;
import com.nokosse.ultimaratio.entity.KeraxEntity;
import com.nokosse.ultimaratio.entity.MambaEntity;
import com.nokosse.ultimaratio.entity.RafaleEntity;
import com.nokosse.ultimaratio.entity.MambaRadarEntity;
import com.nokosse.ultimaratio.entity.LeclercAzurEntity;
import com.nokosse.ultimaratio.entity.LeclercXlrEntity;
import com.nokosse.ultimaratio.entity.SampTNgEntity;
import com.nokosse.ultimaratio.entity.ThundartEntity;
import com.nokosse.ultimaratio.entity.ServalDsaEntity;
import com.nokosse.ultimaratio.entity.ServalEntity;
import com.nokosse.ultimaratio.entity.VbmrMepacEntity;
import com.nokosse.ultimaratio.entity.VbmrEntity;
import com.nokosse.ultimaratio.entity.projectile.Aster30MissileEntity;
import com.nokosse.ultimaratio.entity.projectile.MicaEntity;
import com.nokosse.ultimaratio.entity.projectile.ScalpEgEntity;
import com.nokosse.ultimaratio.entity.projectile.HadesMissileEntity;
import com.nokosse.ultimaratio.entity.projectile.ThundartRocketEntity;
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

    public static final RegistryObject<EntityType<HadesEntity>> HADES = register(
            "hades",
            EntityType.Builder.<HadesEntity>of(HadesEntity::new, MobCategory.MISC)
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

    public static final RegistryObject<EntityType<SampTNgEntity>> SAMP_T_NG = register(
            "samp_t_ng",
            EntityType.Builder.<SampTNgEntity>of(SampTNgEntity::new, MobCategory.MISC)
                    .setTrackingRange(512)
                    .setUpdateInterval(1)
                    .fireImmune()
                    .sized(4.5F, 3.4F)
    );

    public static final RegistryObject<EntityType<ThundartEntity>> THUNDART = register(
            "thundart",
            EntityType.Builder.<ThundartEntity>of(ThundartEntity::new, MobCategory.MISC)
                    .setTrackingRange(512)
                    .setUpdateInterval(1)
                    .fireImmune()
                    .sized(4.5F, 3.4F)
    );

    public static final RegistryObject<EntityType<KeraxEntity>> RENAULT_KERAX = register(
            "renault_kerax",
            EntityType.Builder.<KeraxEntity>of(KeraxEntity::new, MobCategory.MISC)
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

    public static final RegistryObject<EntityType<ServalEntity>> SERVAL = register(
            "serval",
            EntityType.Builder.<ServalEntity>of(ServalEntity::new, MobCategory.MISC)
                    .setTrackingRange(512)
                    .setUpdateInterval(1)
                    .fireImmune()
                    .sized(3.6F, 3.0F)
    );

    public static final RegistryObject<EntityType<ServalDsaEntity>> SERVAL_DSA = register(
            "serval_dsa",
            EntityType.Builder.<ServalDsaEntity>of(ServalDsaEntity::new, MobCategory.MISC)
                    .setTrackingRange(512)
                    .setUpdateInterval(1)
                    .fireImmune()
                    .sized(3.6F, 3.0F)
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

    public static final RegistryObject<EntityType<LeclercAzurEntity>> LECLERC_AZUR = register(
            "leclerc_azur",
            EntityType.Builder.<LeclercAzurEntity>of(LeclercAzurEntity::new, MobCategory.MISC)
                    .setTrackingRange(512)
                    .setUpdateInterval(1)
                    .fireImmune()
                    .sized(5.6F, 3.5F)
    );

    public static final RegistryObject<EntityType<RafaleEntity>> RAFALE_C = register(
            "rafale_c",
            EntityType.Builder.<RafaleEntity>of(RafaleEntity::new, MobCategory.MISC)
                    .setTrackingRange(512)
                    .setUpdateInterval(1)
                    .fireImmune()
                    .sized(3.375F, 2.625F)
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

    public static final RegistryObject<EntityType<MicaEntity>> MICA = register(
            "mica",
            EntityType.Builder.<MicaEntity>of(MicaEntity::new, MobCategory.MISC)
                    .setTrackingRange(512)
                    .setUpdateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .fireImmune()
                    .sized(0.5F, 0.5F)
    );

    public static final RegistryObject<EntityType<ThundartRocketEntity>> THUNDART_ROCKET = register(
            "thundart_rocket",
            EntityType.Builder.<ThundartRocketEntity>of(ThundartRocketEntity::new, MobCategory.MISC)
                    .setTrackingRange(512)
                    .setUpdateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .fireImmune()
                    .sized(0.6F, 0.6F)
    );

    public static final RegistryObject<EntityType<HadesMissileEntity>> HADES_MISSILE = register(
            "hades_missile",
            EntityType.Builder.<HadesMissileEntity>of(HadesMissileEntity::new, MobCategory.MISC)
                    .setTrackingRange(512)
                    .setUpdateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .fireImmune()
                    .sized(1.0F, 1.0F)
    );

    public static final RegistryObject<EntityType<ScalpEgEntity>> SCALP_EG = register(
            "scalp_eg",
            EntityType.Builder.<ScalpEgEntity>of(ScalpEgEntity::new, MobCategory.MISC)
                    .setTrackingRange(512)
                    .setUpdateInterval(1)
                    .setShouldReceiveVelocityUpdates(true)
                    .fireImmune()
                    .sized(1.0F, 1.0F)
    );

    private static <T extends Entity> RegistryObject<EntityType<T>> register(
            String name,
            EntityType.Builder<T> builder
    ) {
        return REGISTRY.register(name, () -> builder.build(name));
    }
}
