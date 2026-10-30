package com.nokosse.ultimaratio.init;

import com.nokosse.ultimaratio.UltimaRatio;
import com.nokosse.ultimaratio.inventory.Vsr700Menu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, UltimaRatio.MODID);

    public static final RegistryObject<MenuType<Vsr700Menu>> VSR700 = MENUS.register("vsr700",
            () -> IForgeMenuType.create((id, inventory, buf) -> new Vsr700Menu(id, inventory, buf.readInt())));

    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}
