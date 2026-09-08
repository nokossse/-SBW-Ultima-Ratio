package com.nokosse.ultimaratio.init;

import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.entity.CaesarEntity;
import com.nokosse.ultimaratio.entity.MambaEntity;
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
                    .sized(8.0F, 7.5F)
    );

    private static <T extends Entity> RegistryObject<EntityType<T>> register(
            String name,
            EntityType.Builder<T> builder
    ) {
        return REGISTRY.register(name, () -> builder.build(name));
    }
}
