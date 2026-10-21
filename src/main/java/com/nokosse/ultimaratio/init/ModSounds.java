package com.nokosse.ultimaratio.init;

import com.nokosse.ultimaratio.UltimaRatio;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> REGISTRY =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, UltimaRatio.MODID);

    public static final RegistryObject<SoundEvent> MORTAR_FIRE = REGISTRY.register(
            "mortar_fire",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(UltimaRatio.MODID, "mortar_fire"))
    );

    /** 78 ms clip (60 / 769 RPM): SBW loops it while the trigger is held, so one loop = one round. */
    public static final RegistryObject<SoundEvent> RAFALE_30MM_FIRE = REGISTRY.register(
            "rafale_30mm_fire",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(UltimaRatio.MODID, "rafale_30mm_fire"))
    );

    public static final RegistryObject<SoundEvent> CAESAR_FIRE = REGISTRY.register(
            "caesar_fire",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(UltimaRatio.MODID, "caesar_fire"))
    );
}
