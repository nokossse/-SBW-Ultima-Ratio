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
}
