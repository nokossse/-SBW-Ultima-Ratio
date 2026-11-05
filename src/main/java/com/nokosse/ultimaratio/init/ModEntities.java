package com.nokosse.ultimaratio.init;

import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.entity.As350Entity;
import com.nokosse.ultimaratio.entity.CaesarEntity;
import com.nokosse.ultimaratio.entity.HadesEntity;
import com.nokosse.ultimaratio.entity.KeraxEntity;
import com.nokosse.ultimaratio.entity.LeclercAzurEntity;
import com.nokosse.ultimaratio.entity.LeclercXlrEntity;
import com.nokosse.ultimaratio.entity.MambaEntity;
import com.nokosse.ultimaratio.entity.MambaRadarEntity;
import com.nokosse.ultimaratio.entity.Mq9ReaperEntity;
import com.nokosse.ultimaratio.entity.RafaleEntity;
import com.nokosse.ultimaratio.entity.SampTNgEntity;
import com.nokosse.ultimaratio.entity.ServalDsaEntity;
import com.nokosse.ultimaratio.entity.ServalEntity;
import com.nokosse.ultimaratio.entity.ServalLadEntity;
import com.nokosse.ultimaratio.entity.ThundartEntity;
import com.nokosse.ultimaratio.entity.TigreEntity;
import com.nokosse.ultimaratio.entity.VbmrEntity;
import com.nokosse.ultimaratio.entity.VbmrMepacEntity;
import com.nokosse.ultimaratio.entity.Vsr700Entity;
import com.nokosse.ultimaratio.entity.projectile.Aster30MissileEntity;
import com.nokosse.ultimaratio.entity.projectile.HadesMissileEntity;
import com.nokosse.ultimaratio.entity.projectile.MeteorEntity;
import com.nokosse.ultimaratio.entity.projectile.MicaEntity;
import com.nokosse.ultimaratio.entity.projectile.ScalpEgEntity;
import com.nokosse.ultimaratio.entity.projectile.ThundartRocketEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.List;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> REGISTRY =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, UltimaRatio.MODID);

    // --- vehicles: tracked far and every tick, like SBW's own -----------------------------------------------------

    public static final DeferredHolder<EntityType<?>, EntityType<CaesarEntity>> CAESAR = vehicle("caesar", CaesarEntity::new, 4.5F, 3.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<MambaEntity>> MAMBA = vehicle("mamba", MambaEntity::new, 4.5F, 3.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<HadesEntity>> HADES = vehicle("hades", HadesEntity::new, 4.5F, 3.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<MambaRadarEntity>> MAMBA_RADAR = vehicle("mamba_radar", MambaRadarEntity::new, 4.5F, 3.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<SampTNgEntity>> SAMP_T_NG = vehicle("samp_t_ng", SampTNgEntity::new, 4.5F, 3.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<ThundartEntity>> THUNDART = vehicle("thundart", ThundartEntity::new, 4.5F, 3.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<KeraxEntity>> RENAULT_KERAX = vehicle("renault_kerax", KeraxEntity::new, 4.5F, 3.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<VbmrEntity>> VBMR = vehicle("vbmr", VbmrEntity::new, 4.0F, 3.2F);
    public static final DeferredHolder<EntityType<?>, EntityType<VbmrEntity>> VAB_P = vehicle("vab_p", VbmrEntity::new, 4.2F, 3.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<ServalEntity>> SERVAL = vehicle("serval", ServalEntity::new, 3.6F, 3.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<ServalDsaEntity>> SERVAL_DSA = vehicle("serval_dsa", ServalDsaEntity::new, 3.6F, 3.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<ServalLadEntity>> SERVAL_LAD = vehicle("serval_lad", ServalLadEntity::new, 3.6F, 3.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<VbmrMepacEntity>> VBMR_MEPAC = vehicle("vbmr_mepac", VbmrMepacEntity::new, 4.0F, 3.2F);
    public static final DeferredHolder<EntityType<?>, EntityType<LeclercXlrEntity>> LECLERC_XLR = vehicle("leclerc_xlr", LeclercXlrEntity::new, 5.2F, 3.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<LeclercAzurEntity>> LECLERC_AZUR = vehicle("leclerc_azur", LeclercAzurEntity::new, 5.6F, 3.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<RafaleEntity>> RAFALE_C = vehicle("rafale_c", RafaleEntity::new, 3.375F, 2.625F);
    public static final DeferredHolder<EntityType<?>, EntityType<As350Entity>> AS350 = vehicle("as350", As350Entity::new, 2.1F, 3.1F);
    public static final DeferredHolder<EntityType<?>, EntityType<Vsr700Entity>> VSR700 = vehicle("vsr700", Vsr700Entity::new, 2.0F, 2.4F);
    public static final DeferredHolder<EntityType<?>, EntityType<Mq9ReaperEntity>> MQ9_REAPER = vehicle("mq9_reaper", Mq9ReaperEntity::new, 3.0F, 2.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<TigreEntity>> TIGRE = vehicle("tigre", TigreEntity::new, 3.375F, 3.0F);

    // --- projectiles: same tracking, plus velocity updates so they fly smoothly on clients ------------------------

    public static final DeferredHolder<EntityType<?>, EntityType<Aster30MissileEntity>> ASTER_30_MISSILE = projectile("aster_30_missile", Aster30MissileEntity::new, 0.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<MicaEntity>> MICA = projectile("mica", MicaEntity::new, 0.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<MeteorEntity>> METEOR = projectile("meteor", MeteorEntity::new, 0.5F);
    public static final DeferredHolder<EntityType<?>, EntityType<ThundartRocketEntity>> THUNDART_ROCKET = projectile("thundart_rocket", ThundartRocketEntity::new, 0.6F);
    public static final DeferredHolder<EntityType<?>, EntityType<HadesMissileEntity>> HADES_MISSILE = projectile("hades_missile", HadesMissileEntity::new, 1.0F);
    public static final DeferredHolder<EntityType<?>, EntityType<ScalpEgEntity>> SCALP_EG = projectile("scalp_eg", ScalpEgEntity::new, 1.0F);

    /** Every vehicle, in the order of the creative tab (by role: artillery and air defence, land, air, drones). */
    public static final List<DeferredHolder<EntityType<?>, ? extends EntityType<?>>> VEHICLES = List.of(
            CAESAR, MAMBA, HADES, MAMBA_RADAR, SAMP_T_NG, THUNDART, RENAULT_KERAX,
            VBMR, VBMR_MEPAC, VAB_P, SERVAL, SERVAL_DSA, SERVAL_LAD, LECLERC_XLR, LECLERC_AZUR,
            RAFALE_C, TIGRE, AS350, VSR700, MQ9_REAPER
    );

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> vehicle(String name, EntityType.EntityFactory<T> factory,
                                                                           float width, float height) {
        return REGISTRY.register(name, () -> EntityType.Builder.of(factory, MobCategory.MISC)
                .setTrackingRange(512)
                .setUpdateInterval(1)
                .fireImmune()
                .sized(width, height)
                .build(name));
    }

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> projectile(String name, EntityType.EntityFactory<T> factory,
                                                                              float size) {
        return REGISTRY.register(name, () -> EntityType.Builder.of(factory, MobCategory.MISC)
                .setTrackingRange(512)
                .setUpdateInterval(1)
                .setShouldReceiveVelocityUpdates(true)
                .fireImmune()
                .sized(size, size)
                .build(name));
    }
}
