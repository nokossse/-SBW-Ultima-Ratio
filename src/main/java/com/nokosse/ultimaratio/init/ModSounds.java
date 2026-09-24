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
}
