package com.nokosse.ultimaratio.init;

import com.nokosse.ultimaratio.UltimaRatio;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> REGISTRY =
            DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, UltimaRatio.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> MORTAR_FIRE = REGISTRY.register(
            "mortar_fire",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(UltimaRatio.MODID, "mortar_fire"))
    );

    /** 78 ms clip (60 / 769 RPM): SBW loops it while the trigger is held, so one loop = one round. */
    public static final DeferredHolder<SoundEvent, SoundEvent> RAFALE_30MM_FIRE = REGISTRY.register(
            "rafale_30mm_fire",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(UltimaRatio.MODID, "rafale_30mm_fire"))
    );

    public static final DeferredHolder<SoundEvent, SoundEvent> CAESAR_FIRE = REGISTRY.register(
            "caesar_fire",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(UltimaRatio.MODID, "caesar_fire"))
    );
}
